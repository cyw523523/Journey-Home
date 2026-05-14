package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.AdoptApply;
import com.guitu.domain.AppealRecord;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AppealDecisionAction;
import com.guitu.domain.enums.AppealStatus;
import com.guitu.domain.enums.AppealTargetType;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.dto.AppealDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AppealRecordRepository;
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
public class AppealService {
    private final AppealRecordRepository appealRepository;
    private final UserService userService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final AdoptApplyService adoptApplyService;
    private final CommunityService communityService;
    private final DtoMapper mapper;
    private final AntiAbuseService antiAbuseService;
    private final ContentModerationService moderationService;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;

    public AppealService(
            AppealRecordRepository appealRepository,
            UserService userService,
            AnimalService animalService,
            RescueService rescueService,
            AdoptApplyService adoptApplyService,
            CommunityService communityService,
            DtoMapper mapper,
            AntiAbuseService antiAbuseService,
            ContentModerationService moderationService,
            NotificationService notificationService,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.appealRepository = appealRepository;
        this.userService = userService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.adoptApplyService = adoptApplyService;
        this.communityService = communityService;
        this.mapper = mapper;
        this.antiAbuseService = antiAbuseService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public AppealDtos.AppealResponse create(AppealDtos.CreateAppealRequest request) {
        User applicant = userService.currentUser();
        antiAbuseService.check("appeal", applicant.getId().toString(), 5, Duration.ofDays(1), "Too many appeals, please try again later");
        moderationService.validateText("Appeal reason", request.reason());
        validateAppealEligibility(applicant, request.targetType(), request.targetId());
        boolean duplicated = appealRepository.existsByTargetTypeAndTargetIdAndApplicantIdAndStatusIn(
                request.targetType(),
                request.targetId(),
                applicant.getId(),
                List.of(AppealStatus.PENDING_REVIEW, AppealStatus.SECOND_REVIEW_PENDING)
        );
        if (duplicated) {
            throw new BusinessException("A pending appeal already exists for this target");
        }

        AppealRecord appeal = new AppealRecord();
        appeal.setApplicant(applicant);
        appeal.setTargetType(request.targetType());
        appeal.setTargetId(request.targetId());
        appeal.setReason(request.reason().trim());
        AppealRecord saved = appealRepository.save(appeal);
        notificationService.notifyAdmins(
                NotificationType.APPEAL_CREATED,
                "新申诉待处理",
                "有新的申诉需要管理员审核。",
                request.targetType().name(),
                saved.getId()
        );
        return mapper.toAppealResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppealDtos.AppealResponse> listMine(int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Page<AppealRecord> result = appealRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("applicant").get("id"), userId),
                pageRequest(page, size)
        );
        return PageResponse.from(result, mapper::toAppealResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppealDtos.AppealResponse> adminList(AppealStatus status, AppealTargetType targetType, int page, int size) {
        Page<AppealRecord> result = appealRepository.findAll(spec(status, targetType), pageRequest(page, size));
        return PageResponse.from(result, mapper::toAppealResponse);
    }

    @Transactional(readOnly = true)
    public AppealDtos.AppealResponse detail(Long id) {
        return mapper.toAppealResponse(getEntity(id));
    }

    @Transactional
    public AppealDtos.AppealResponse review(Long id, AppealDtos.ReviewAppealRequest request) {
        AppealRecord appeal = getEntity(id);
        User reviewer = userService.currentUser();
        if (appeal.getStatus() == AppealStatus.APPROVED || appeal.getStatus() == AppealStatus.REJECTED) {
            throw new BusinessException("This appeal has already been finalized");
        }

        if (request.action() == AppealDecisionAction.ESCALATE) {
            if (appeal.getStatus() != AppealStatus.PENDING_REVIEW) {
                throw new BusinessException("Only first-round appeals can be escalated");
            }
            appeal.setFirstReviewer(reviewer);
            appeal.setFirstReviewOpinion(request.opinion());
            appeal.setStatus(AppealStatus.SECOND_REVIEW_PENDING);
            notificationService.notifyUser(appeal.getApplicant(), NotificationType.APPEAL_UPDATE, "申诉已升级至二审", request.opinion(), appeal.getTargetType().name(), appeal.getTargetId());
            return mapper.toAppealResponse(appeal);
        }

        if (appeal.getStatus() == AppealStatus.SECOND_REVIEW_PENDING) {
            if (appeal.getFirstReviewer() != null && appeal.getFirstReviewer().getId().equals(reviewer.getId())) {
                throw new BusinessException("A different admin must complete the second review");
            }
            appeal.setSecondReviewer(reviewer);
            appeal.setFinalReviewOpinion(request.opinion());
        } else {
            appeal.setFirstReviewer(reviewer);
            appeal.setFirstReviewOpinion(request.opinion());
        }
        appeal.setReviewedAt(LocalDateTime.now());

        if (request.action() == AppealDecisionAction.APPROVE) {
            applyAppealRemedy(appeal);
            appeal.setStatus(AppealStatus.APPROVED);
        } else {
            appeal.setStatus(AppealStatus.REJECTED);
        }

        notificationService.notifyUser(
                appeal.getApplicant(),
                NotificationType.APPEAL_UPDATE,
                "申诉审核结果已更新",
                request.opinion(),
                appeal.getTargetType().name(),
                appeal.getTargetId()
        );
        cacheInvalidationService.evictPublicCaches();
        return mapper.toAppealResponse(appeal);
    }

    @Transactional(readOnly = true)
    public AppealRecord getEntity(Long id) {
        return appealRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Appeal does not exist"));
    }

    private void validateAppealEligibility(User applicant, AppealTargetType targetType, Long targetId) {
        switch (targetType) {
            case ANIMAL -> {
                var animal = animalService.getEntity(targetId);
                if (!animal.getPublisher().getId().equals(applicant.getId()) || (animal.getStatus() != AnimalStatus.REJECTED && animal.getStatus() != AnimalStatus.OFFLINE)) {
                    throw new BusinessException("This animal profile is not eligible for appeal");
                }
            }
            case RESCUE -> {
                var rescue = rescueService.getEntity(targetId);
                if (!rescue.getPublisher().getId().equals(applicant.getId()) || (rescue.getStatus() != RescueStatus.REJECTED && rescue.getStatus() != RescueStatus.OFFLINE)) {
                    throw new BusinessException("This rescue post is not eligible for appeal");
                }
            }
            case ADOPT_APPLY -> {
                AdoptApply apply = adoptApplyService.getEntity(targetId);
                if (!apply.getApplicant().getId().equals(applicant.getId()) || apply.getStatus() != ApplyStatus.REJECTED) {
                    throw new BusinessException("This adoption application is not eligible for appeal");
                }
            }
            case COMMUNITY_POST -> {
                var post = communityService.getManagedPost(targetId);
                if (!post.getAuthor().getId().equals(applicant.getId()) || (post.getStatus() != CommunityPostStatus.REJECTED && post.getStatus() != CommunityPostStatus.OFFLINE)) {
                    throw new BusinessException("This community post is not eligible for appeal");
                }
            }
            case COMMUNITY_COMMENT -> {
                var comment = communityService.getManagedComment(targetId);
                if (!comment.getAuthor().getId().equals(applicant.getId()) || (comment.getStatus() != CommunityCommentStatus.REJECTED && comment.getStatus() != CommunityCommentStatus.OFFLINE)) {
                    throw new BusinessException("This community comment is not eligible for appeal");
                }
            }
            case USER_BAN -> throw new BusinessException("User-ban appeals are not supported in this version");
        }
    }

    private void applyAppealRemedy(AppealRecord appeal) {
        switch (appeal.getTargetType()) {
            case ANIMAL -> {
                var animal = animalService.getEntity(appeal.getTargetId());
                animal.setStatus(AnimalStatus.PENDING_REVIEW);
                animal.setReviewComment("Appeal approved. Returned to review queue.");
            }
            case RESCUE -> {
                var rescue = rescueService.getEntity(appeal.getTargetId());
                rescue.setStatus(RescueStatus.PENDING_REVIEW);
                rescue.setReviewComment("Appeal approved. Returned to review queue.");
            }
            case ADOPT_APPLY -> {
                AdoptApply apply = adoptApplyService.getEntity(appeal.getTargetId());
                if (apply.getAnimal().getStatus() != AnimalStatus.WAITING_ADOPTION) {
                    throw new BusinessException("The related animal is no longer open for adoption");
                }
                apply.setStatus(ApplyStatus.PENDING_REVIEW);
                apply.setAuditOpinion("Appeal approved. Returned to review queue.");
            }
            case COMMUNITY_POST -> communityService.getManagedPost(appeal.getTargetId()).setStatus(CommunityPostStatus.PENDING_REVIEW);
            case COMMUNITY_COMMENT -> communityService.getManagedComment(appeal.getTargetId()).setStatus(CommunityCommentStatus.PENDING_REVIEW);
            case USER_BAN -> throw new BusinessException("User-ban appeals are not supported in this version");
        }
    }

    private Specification<AppealRecord> spec(AppealStatus status, AppealTargetType targetType) {
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
