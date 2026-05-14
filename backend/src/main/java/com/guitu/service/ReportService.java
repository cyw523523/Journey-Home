package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.ContentReport;
import com.guitu.domain.User;
import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.ReportResolutionAction;
import com.guitu.domain.enums.ReportStatus;
import com.guitu.domain.enums.ReportTargetType;
import com.guitu.domain.enums.UserStatus;
import com.guitu.dto.ReportDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.ContentReportRepository;
import com.guitu.security.SecuritySupport;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {
    private final ContentReportRepository reportRepository;
    private final UserService userService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final CommunityService communityService;
    private final DtoMapper mapper;
    private final AntiAbuseService antiAbuseService;
    private final ContentModerationService moderationService;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;

    public ReportService(
            ContentReportRepository reportRepository,
            UserService userService,
            AnimalService animalService,
            RescueService rescueService,
            CommunityService communityService,
            DtoMapper mapper,
            AntiAbuseService antiAbuseService,
            ContentModerationService moderationService,
            NotificationService notificationService,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.communityService = communityService;
        this.mapper = mapper;
        this.antiAbuseService = antiAbuseService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public ReportDtos.ReportResponse create(ReportDtos.CreateReportRequest request) {
        User reporter = userService.currentUser();
        antiAbuseService.check("report", reporter.getId().toString(), 8, Duration.ofHours(2), "Too many reports, please try again later");
        moderationService.validateText("Report description", request.description());

        User targetOwner = resolveTargetOwner(request.targetType(), request.targetId());
        if (targetOwner != null && targetOwner.getId().equals(reporter.getId())) {
            throw new BusinessException("You cannot report your own content");
        }
        boolean duplicated = reportRepository.existsByTargetTypeAndTargetIdAndReporterIdAndStatusIn(
                request.targetType(),
                request.targetId(),
                reporter.getId(),
                List.of(ReportStatus.PENDING_REVIEW, ReportStatus.PROCESSING)
        );
        if (duplicated) {
            throw new BusinessException("You have already submitted a pending report for this target");
        }

        ContentReport report = new ContentReport();
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReporter(reporter);
        report.setTargetOwner(targetOwner);
        report.setReasonType(request.reasonType());
        report.setDescription(request.description().trim());
        report.getEvidenceImageUrls().clear();
        if (request.evidenceImageUrls() != null) {
            report.getEvidenceImageUrls().addAll(request.evidenceImageUrls());
        }
        ContentReport saved = reportRepository.save(report);
        notificationService.notifyAdmins(
                NotificationType.REPORT_CREATED,
                "New report pending review",
                "A new report requires admin review.",
                request.targetType().name(),
                saved.getId()
        );
        return mapper.toReportResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDtos.ReportResponse> listMine(int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Page<ContentReport> result = reportRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("reporter").get("id"), userId),
                pageRequest(page, size)
        );
        return PageResponse.from(result, mapper::toReportResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDtos.ReportResponse> adminList(ReportStatus status, ReportTargetType targetType, int page, int size) {
        Page<ContentReport> result = reportRepository.findAll(spec(status, targetType), pageRequest(page, size));
        return PageResponse.from(result, mapper::toReportResponse);
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportResponse detail(Long id) {
        return mapper.toReportResponse(getEntity(id));
    }

    @Transactional
    public ReportDtos.ReportResponse resolve(Long id, ReportDtos.ResolveReportRequest request) {
        ContentReport report = getEntity(id);
        if (report.getStatus() != ReportStatus.PENDING_REVIEW && report.getStatus() != ReportStatus.PROCESSING) {
            throw new BusinessException("This report has already been resolved");
        }
        User reviewer = userService.currentUser();
        report.setReviewer(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        report.setResolutionAction(request.action());
        report.setResolutionOpinion(request.opinion());

        switch (request.action()) {
            case DISMISS -> report.setStatus(ReportStatus.RESOLVED_INVALID);
            case WARN_ONLY -> report.setStatus(ReportStatus.RESOLVED_VALID);
            case OFFLINE_CONTENT -> {
                applyOffline(report.getTargetType(), report.getTargetId());
                report.setStatus(ReportStatus.RESOLVED_VALID);
            }
            case BAN_USER -> {
                User targetUser = resolvePunishableUser(report);
                if (targetUser == null) {
                    throw new BusinessException("This report target does not support user ban actions");
                }
                targetUser.setStatus(UserStatus.DISABLED);
                report.setStatus(ReportStatus.RESOLVED_VALID);
                notificationService.notifyUser(
                        targetUser,
                        NotificationType.ACCOUNT_ACTION,
                        "Account disabled",
                        request.opinion(),
                        "USER",
                        targetUser.getId()
                );
            }
        }

        notificationService.notifyUser(
                report.getReporter(),
                NotificationType.REPORT_UPDATE,
                "Report processed",
                request.opinion(),
                report.getTargetType().name(),
                report.getTargetId()
        );
        if (report.getTargetOwner() != null && request.action() != ReportResolutionAction.DISMISS) {
            notificationService.notifyUser(
                    report.getTargetOwner(),
                    NotificationType.REPORT_UPDATE,
                    "Content moderation result updated",
                    request.opinion(),
                    report.getTargetType().name(),
                    report.getTargetId()
            );
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toReportResponse(report);
    }

    @Transactional(readOnly = true)
    public ContentReport getEntity(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Report does not exist"));
    }

    private void applyOffline(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case ANIMAL -> animalService.getEntity(targetId).setStatus(com.guitu.domain.enums.AnimalStatus.OFFLINE);
            case RESCUE -> rescueService.getEntity(targetId).setStatus(com.guitu.domain.enums.RescueStatus.OFFLINE);
            case COMMUNITY_POST -> communityService.getManagedPost(targetId).setStatus(CommunityPostStatus.OFFLINE);
            case COMMUNITY_COMMENT -> communityService.getManagedComment(targetId).setStatus(CommunityCommentStatus.OFFLINE);
            case USER -> userService.getById(targetId).setStatus(UserStatus.DISABLED);
        }
    }

    private User resolvePunishableUser(ContentReport report) {
        return switch (report.getTargetType()) {
            case USER -> userService.getById(report.getTargetId());
            default -> report.getTargetOwner();
        };
    }

    private User resolveTargetOwner(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case ANIMAL -> animalService.getEntity(targetId).getPublisher();
            case RESCUE -> rescueService.getEntity(targetId).getPublisher();
            case COMMUNITY_POST -> communityService.getManagedPost(targetId).getAuthor();
            case COMMUNITY_COMMENT -> communityService.getManagedComment(targetId).getAuthor();
            case USER -> userService.getById(targetId);
        };
    }

    private Specification<ContentReport> spec(ReportStatus status, ReportTargetType targetType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
