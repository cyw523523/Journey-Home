package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.AuditLog;
import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.Rescue;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.AuditAction;
import com.guitu.domain.enums.AuditTargetType;
import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.dto.AuditDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AdoptApplyRepository;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.AuditLogRepository;
import com.guitu.repository.CommunityCommentRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.RescueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AnimalRepository animalRepository;
    private final RescueRepository rescueRepository;
    private final AdoptApplyRepository adoptApplyRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final AdoptApplyService adoptApplyService;
    private final CommunityService communityService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;
    private final OperationLogService operationLogService;
    private final AdoptionExtensionService adoptionExtensionService;

    public AuditService(
            AnimalRepository animalRepository,
            RescueRepository rescueRepository,
            AdoptApplyRepository adoptApplyRepository,
            CommunityPostRepository communityPostRepository,
            CommunityCommentRepository communityCommentRepository,
            AuditLogRepository auditLogRepository,
            UserService userService,
            AnimalService animalService,
            RescueService rescueService,
            AdoptApplyService adoptApplyService,
            CommunityService communityService,
            DtoMapper mapper,
            NotificationService notificationService,
            CacheInvalidationService cacheInvalidationService,
            OperationLogService operationLogService,
            AdoptionExtensionService adoptionExtensionService
    ) {
        this.animalRepository = animalRepository;
        this.rescueRepository = rescueRepository;
        this.adoptApplyRepository = adoptApplyRepository;
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.adoptApplyService = adoptApplyService;
        this.communityService = communityService;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.operationLogService = operationLogService;
        this.adoptionExtensionService = adoptionExtensionService;
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.PendingItemResponse> pending(AuditTargetType targetType, int page, int size) {
        if (targetType == AuditTargetType.ANIMAL) {
            return animalRepository.findAll(animalPendingSpec(), pageRequest(page, size)).getContent().stream().map(this::pendingAnimal).toList();
        }
        if (targetType == AuditTargetType.RESCUE) {
            return rescueRepository.findAll(rescuePendingSpec(), pageRequest(page, size)).getContent().stream().map(this::pendingRescue).toList();
        }
        if (targetType == AuditTargetType.ADOPT_APPLY) {
            return adoptApplyRepository.findAll(applyPendingSpec(), pageRequest(page, size)).getContent().stream().map(this::pendingApply).toList();
        }
        if (targetType == AuditTargetType.COMMUNITY_POST) {
            return communityPostRepository.findAll(postPendingSpec(), pageRequest(page, size)).getContent().stream().map(this::pendingPost).toList();
        }
        if (targetType == AuditTargetType.COMMUNITY_COMMENT) {
            return communityCommentRepository.findAll(commentPendingSpec(), pageRequest(page, size)).getContent().stream().map(this::pendingComment).toList();
        }

        List<AuditDtos.PendingItemResponse> result = new ArrayList<>();
        result.addAll(animalRepository.findAll(animalPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingAnimal).toList());
        result.addAll(rescueRepository.findAll(rescuePendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingRescue).toList());
        result.addAll(adoptApplyRepository.findAll(applyPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingApply).toList());
        result.addAll(communityPostRepository.findAll(postPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingPost).toList());
        result.addAll(communityCommentRepository.findAll(commentPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingComment).toList());
        return result.stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .limit(Math.max(1, size))
                .toList();
    }

    @Transactional(readOnly = true)
    public Object detail(AuditTargetType targetType, Long targetId) {
        return switch (targetType) {
            case ANIMAL -> mapper.toAnimalResponse(animalService.getEntity(targetId));
            case RESCUE -> mapper.toRescueResponse(rescueService.getEntity(targetId));
            case ADOPT_APPLY -> mapper.toApplyResponse(adoptApplyService.getEntity(targetId));
            case COMMUNITY_POST -> mapper.toCommunityPostResponse(communityService.getManagedPost(targetId), communityCommentRepository.countByPostIdAndStatus(targetId, CommunityCommentStatus.PUBLISHED));
            case COMMUNITY_COMMENT -> mapper.toCommunityCommentResponse(communityService.getManagedComment(targetId));
        };
    }

    @Transactional
    public void audit(AuditDtos.AuditRequest request) {
        User auditor = userService.currentUser();
        switch (request.targetType()) {
            case ANIMAL -> auditAnimal(request, auditor);
            case RESCUE -> auditRescue(request, auditor);
            case ADOPT_APPLY -> auditApply(request, auditor);
            case COMMUNITY_POST -> auditPost(request, auditor);
            case COMMUNITY_COMMENT -> auditComment(request, auditor);
        }
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDtos.AuditLogResponse> logs(AuditTargetType targetType, Long targetId, int page, int size) {
        Page<AuditLog> result;
        if (targetType != null && targetId != null) {
            result = auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId, pageRequest(page, size));
        } else {
            result = auditLogRepository.findAll(pageRequest(page, size));
        }
        return PageResponse.from(result, mapper::toAuditLogResponse);
    }

    private void auditAnimal(AuditDtos.AuditRequest request, User auditor) {
        Animal animal = animalService.getEntity(request.targetId());
        AnimalStatus beforeStatus = animal.getStatus();
        if (request.action() == AuditAction.OFFLINE) {
            animal.setStatus(AnimalStatus.OFFLINE);
        } else {
            if (animal.getStatus() != AnimalStatus.PENDING_REVIEW) {
                throw new BusinessException("This record has already been audited");
            }
            boolean isStatusUpdate = animal.getReviewComment() != null && animal.getReviewComment().startsWith("STATUS_UPDATE|");
            if (request.action() == AuditAction.APPROVE) {
                if (isStatusUpdate) {
                    String[] parts = animal.getReviewComment().split("\\|");
                    if (parts.length == 3) {
                        try { animal.setStatus(AnimalStatus.valueOf(parts[2])); }
                        catch (IllegalArgumentException e) { animal.setStatus(AnimalStatus.WAITING_ADOPTION); }
                    } else { animal.setStatus(AnimalStatus.WAITING_ADOPTION); }
                } else {
                    animal.setStatus(AnimalStatus.WAITING_ADOPTION);
                }
            } else {
                if (isStatusUpdate) {
                    String[] parts = animal.getReviewComment().split("\\|");
                    if (parts.length >= 2) {
                        try { animal.setStatus(AnimalStatus.valueOf(parts[1])); }
                        catch (IllegalArgumentException e) { animal.setStatus(AnimalStatus.REJECTED); }
                    } else { animal.setStatus(AnimalStatus.REJECTED); }
                } else {
                    animal.setStatus(AnimalStatus.REJECTED);
                }
            }
        }
        animal.setReviewComment(request.opinion());
        recordLog(AuditTargetType.ANIMAL, animal.getId(), auditor, request.action(), request.opinion());
        operationLogService.record(
                auditor,
                OperationTargetType.ANIMAL,
                animal.getId(),
                animal.getType().getLabel() + " / " + animal.getFoundRegion(),
                request.action() == AuditAction.OFFLINE ? OperationType.OFFLINE : OperationType.STATUS_CHANGE,
                "管理员审核动物档案",
                java.util.Map.of("status", beforeStatus.name()),
                java.util.Map.of("status", animal.getStatus().name(), "reviewComment", animal.getReviewComment())
        );
        notifyAuditResult(animal.getPublisher(), "AUDIT_RESULT_ANIMAL", request.opinion(), "ANIMAL", animal.getId());
    }

    private void auditRescue(AuditDtos.AuditRequest request, User auditor) {
        Rescue rescue = rescueService.getEntity(request.targetId());
        RescueStatus beforeStatus = rescue.getStatus();
        if (request.action() == AuditAction.OFFLINE) {
            rescue.setStatus(RescueStatus.OFFLINE);
        } else {
            if (rescue.getStatus() != RescueStatus.PENDING_REVIEW) {
                throw new BusinessException("This record has already been audited");
            }
            rescue.setStatus(request.action() == AuditAction.APPROVE ? RescueStatus.PENDING_PROCESS : RescueStatus.REJECTED);
        }
        rescue.setReviewComment(request.opinion());
        recordLog(AuditTargetType.RESCUE, rescue.getId(), auditor, request.action(), request.opinion());
        operationLogService.record(
                auditor,
                OperationTargetType.RESCUE,
                rescue.getId(),
                rescue.getLocation(),
                request.action() == AuditAction.OFFLINE ? OperationType.OFFLINE : OperationType.STATUS_CHANGE,
                "管理员审核救助信息",
                java.util.Map.of("status", beforeStatus.name()),
                java.util.Map.of("status", rescue.getStatus().name(), "reviewComment", rescue.getReviewComment())
        );
        notifyAuditResult(rescue.getPublisher(), "AUDIT_RESULT_RESCUE", request.opinion(), "RESCUE", rescue.getId());
    }

    private void auditApply(AuditDtos.AuditRequest request, User auditor) {
        if (request.action() == AuditAction.OFFLINE) {
            throw new BusinessException("Adoption applications do not support offline actions");
        }
        AdoptApply apply = adoptApplyService.getEntity(request.targetId());
        if (apply.getStatus() != ApplyStatus.PENDING_REVIEW) {
            throw new BusinessException("This record has already been audited");
        }
        ApplyStatus beforeStatus = apply.getStatus();
        if (request.action() == AuditAction.APPROVE) {
            if (apply.getAnimal().getStatus() != AnimalStatus.WAITING_ADOPTION) {
                throw new BusinessException("This animal is not available for adoption right now");
            }
            apply.setStatus(ApplyStatus.APPROVED);
            apply.setAuditOpinion(request.opinion());
            apply.getAnimal().setStatus(AnimalStatus.ADOPTED);
            rejectOtherPendingApplies(apply);
            try {
                adoptionExtensionService.initializeForApprovedApply(apply, auditor, request.opinion());
            } catch (RuntimeException ex) {
                log.error("Failed to initialize adoption extension for apply {}", apply.getId(), ex);
            }
        } else {
            apply.setStatus(ApplyStatus.REJECTED);
            apply.setAuditOpinion(request.opinion());
        }
        recordLog(AuditTargetType.ADOPT_APPLY, apply.getId(), auditor, request.action(), request.opinion());
        operationLogService.record(
                auditor,
                OperationTargetType.ADOPT_APPLY,
                apply.getId(),
                "领养申请#" + apply.getId() + " / " + apply.getAnimal().getType().getLabel(),
                request.action() == AuditAction.APPROVE ? OperationType.APPROVE_APPLICATION : OperationType.REJECT_APPLICATION,
                "管理员审核领养申请",
                java.util.Map.of("status", beforeStatus.name()),
                java.util.Map.of("status", apply.getStatus().name(), "auditOpinion", apply.getAuditOpinion())
        );
        notifyAuditResult(apply.getApplicant(), "AUDIT_RESULT_ADOPT_APPLY", request.opinion(), "ADOPT_APPLY", apply.getId());
    }

    private void auditPost(AuditDtos.AuditRequest request, User auditor) {
        CommunityPost post = communityService.getManagedPost(request.targetId());
        CommunityPostStatus beforeStatus = post.getStatus();
        if (request.action() == AuditAction.OFFLINE) {
            post.setStatus(CommunityPostStatus.OFFLINE);
        } else {
            if (post.getStatus() != CommunityPostStatus.PENDING_REVIEW) {
                throw new BusinessException("This community post has already been reviewed");
            }
            post.setStatus(request.action() == AuditAction.APPROVE ? CommunityPostStatus.PUBLISHED : CommunityPostStatus.REJECTED);
        }
        recordLog(AuditTargetType.COMMUNITY_POST, post.getId(), auditor, request.action(), request.opinion());
        operationLogService.record(
                auditor,
                OperationTargetType.COMMUNITY_POST,
                post.getId(),
                post.getTitle(),
                request.action() == AuditAction.OFFLINE ? OperationType.OFFLINE : OperationType.STATUS_CHANGE,
                "管理员审核社区帖子",
                java.util.Map.of("status", beforeStatus.name()),
                java.util.Map.of("status", post.getStatus().name(), "opinion", request.opinion())
        );
        notifyAuditResult(post.getAuthor(), "AUDIT_RESULT_COMMUNITY_POST", request.opinion(), "COMMUNITY_POST", post.getId());
        if (request.action() == AuditAction.APPROVE && post.getCategory() != null) {
            post.getCategory().setPostCount(post.getCategory().getPostCount() + 1);
        }
    }

    private void auditComment(AuditDtos.AuditRequest request, User auditor) {
        CommunityComment comment = communityService.getManagedComment(request.targetId());
        CommunityCommentStatus beforeStatus = comment.getStatus();
        if (request.action() == AuditAction.OFFLINE) {
            comment.setStatus(CommunityCommentStatus.OFFLINE);
        } else {
            if (comment.getStatus() != CommunityCommentStatus.PENDING_REVIEW) {
                throw new BusinessException("This community comment has already been reviewed");
            }
            comment.setStatus(request.action() == AuditAction.APPROVE ? CommunityCommentStatus.PUBLISHED : CommunityCommentStatus.REJECTED);
        }
        recordLog(AuditTargetType.COMMUNITY_COMMENT, comment.getId(), auditor, request.action(), request.opinion());
        operationLogService.record(
                auditor,
                OperationTargetType.COMMUNITY_COMMENT,
                comment.getId(),
                "评论#" + comment.getId() + " / 帖子#" + comment.getPost().getId(),
                request.action() == AuditAction.OFFLINE ? OperationType.OFFLINE : OperationType.STATUS_CHANGE,
                "管理员审核社区评论",
                java.util.Map.of("status", beforeStatus.name()),
                java.util.Map.of("status", comment.getStatus().name(), "opinion", request.opinion())
        );
        notifyAuditResult(comment.getAuthor(), "AUDIT_RESULT_COMMUNITY_COMMENT", request.opinion(), "COMMUNITY_COMMENT", comment.getId());
    }

    private void rejectOtherPendingApplies(AdoptApply approvedApply) {
        List<AdoptApply> pendingApplies = adoptApplyRepository.findByAnimalIdAndStatusAndIdNot(
                approvedApply.getAnimal().getId(),
                ApplyStatus.PENDING_REVIEW,
                approvedApply.getId()
        );
        for (AdoptApply pendingApply : pendingApplies) {
            pendingApply.setStatus(ApplyStatus.REJECTED);
            pendingApply.setAuditOpinion("该动物已有其他领养申请被批准");
            notifyAuditResult(
                    pendingApply.getApplicant(),
                    "AUDIT_RESULT_ADOPT_APPLY_AUTO_REJECT",
                    pendingApply.getAuditOpinion(),
                    "ADOPT_APPLY",
                    pendingApply.getId()
            );
        }
    }

    private void notifyAuditResult(User recipient, String title, String opinion, String relatedType, Long relatedId) {
        notificationService.notifyUser(
                recipient,
                NotificationType.AUDIT_RESULT,
                title,
                opinion,
                relatedType,
                relatedId
        );
    }

    private void recordLog(AuditTargetType targetType, Long targetId, User auditor, AuditAction action, String opinion) {
        AuditLog log = new AuditLog();
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAuditor(auditor);
        log.setAction(action);
        log.setOpinion(opinion);
        log.setAuditTime(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private AuditDtos.PendingItemResponse pendingAnimal(Animal animal) {
        return new AuditDtos.PendingItemResponse(AuditTargetType.ANIMAL, animal.getId(), animal.getType().getLabel() + " / " + animal.getFoundRegion(), animal.getStatus().getLabel(), animal.getPublisher().getNickname(), animal.getCreatedAt());
    }

    private AuditDtos.PendingItemResponse pendingRescue(Rescue rescue) {
        return new AuditDtos.PendingItemResponse(AuditTargetType.RESCUE, rescue.getId(), rescue.getLocation() + " / " + rescue.getAnimalCondition(), rescue.getStatus().getLabel(), rescue.getPublisher().getNickname(), rescue.getCreatedAt());
    }

    private AuditDtos.PendingItemResponse pendingApply(AdoptApply apply) {
        return new AuditDtos.PendingItemResponse(AuditTargetType.ADOPT_APPLY, apply.getId(), "Adoption application #" + apply.getId(), apply.getStatus().getLabel(), apply.getApplicantName(), apply.getCreatedAt());
    }

    private AuditDtos.PendingItemResponse pendingPost(CommunityPost post) {
        return new AuditDtos.PendingItemResponse(AuditTargetType.COMMUNITY_POST, post.getId(), post.getTitle(), post.getStatus().getLabel(), post.getAuthor().getNickname(), post.getCreatedAt());
    }

    private AuditDtos.PendingItemResponse pendingComment(CommunityComment comment) {
        return new AuditDtos.PendingItemResponse(AuditTargetType.COMMUNITY_COMMENT, comment.getId(), "Comment on post #" + comment.getPost().getId(), comment.getStatus().getLabel(), comment.getAuthor().getNickname(), comment.getCreatedAt());
    }

    private Specification<Animal> animalPendingSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), AnimalStatus.PENDING_REVIEW);
    }

    private Specification<Rescue> rescuePendingSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), RescueStatus.PENDING_REVIEW);
    }

    private Specification<AdoptApply> applyPendingSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), ApplyStatus.PENDING_REVIEW);
    }

    private Specification<CommunityPost> postPendingSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), CommunityPostStatus.PENDING_REVIEW);
    }

    private Specification<CommunityComment> commentPendingSpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), CommunityCommentStatus.PENDING_REVIEW);
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
