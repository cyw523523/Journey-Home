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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {
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
            CacheInvalidationService cacheInvalidationService
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
        if (request.action() == AuditAction.OFFLINE) {
            animal.setStatus(AnimalStatus.OFFLINE);
        } else {
            if (animal.getStatus() != AnimalStatus.PENDING_REVIEW) {
                throw new BusinessException("This record has already been audited");
            }
            animal.setStatus(request.action() == AuditAction.APPROVE ? AnimalStatus.WAITING_ADOPTION : AnimalStatus.REJECTED);
        }
        animal.setReviewComment(request.opinion());
        recordLog(AuditTargetType.ANIMAL, animal.getId(), auditor, request.action(), request.opinion());
        notifyAuditResult(animal.getPublisher(), "Animal profile review updated", request.opinion(), "ANIMAL", animal.getId());
    }

    private void auditRescue(AuditDtos.AuditRequest request, User auditor) {
        Rescue rescue = rescueService.getEntity(request.targetId());
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
        notifyAuditResult(rescue.getPublisher(), "Rescue post review updated", request.opinion(), "RESCUE", rescue.getId());
    }

    private void auditApply(AuditDtos.AuditRequest request, User auditor) {
        if (request.action() == AuditAction.OFFLINE) {
            throw new BusinessException("Adoption applications do not support offline actions");
        }
        AdoptApply apply = adoptApplyService.getEntity(request.targetId());
        if (apply.getStatus() != ApplyStatus.PENDING_REVIEW) {
            throw new BusinessException("This record has already been audited");
        }
        if (request.action() == AuditAction.APPROVE) {
            if (apply.getAnimal().getStatus() != AnimalStatus.WAITING_ADOPTION) {
                throw new BusinessException("This animal is not available for adoption right now");
            }
            apply.setStatus(ApplyStatus.APPROVED);
            apply.setAuditOpinion(request.opinion());
            apply.getAnimal().setStatus(AnimalStatus.ADOPTED);
            rejectOtherPendingApplies(apply);
        } else {
            apply.setStatus(ApplyStatus.REJECTED);
            apply.setAuditOpinion(request.opinion());
        }
        recordLog(AuditTargetType.ADOPT_APPLY, apply.getId(), auditor, request.action(), request.opinion());
        notifyAuditResult(apply.getApplicant(), "Adoption application review updated", request.opinion(), "ADOPT_APPLY", apply.getId());
    }

    private void auditPost(AuditDtos.AuditRequest request, User auditor) {
        CommunityPost post = communityService.getManagedPost(request.targetId());
        if (request.action() == AuditAction.OFFLINE) {
            post.setStatus(CommunityPostStatus.OFFLINE);
        } else {
            if (post.getStatus() != CommunityPostStatus.PENDING_REVIEW) {
                throw new BusinessException("This community post has already been reviewed");
            }
            post.setStatus(request.action() == AuditAction.APPROVE ? CommunityPostStatus.PUBLISHED : CommunityPostStatus.REJECTED);
        }
        recordLog(AuditTargetType.COMMUNITY_POST, post.getId(), auditor, request.action(), request.opinion());
        notifyAuditResult(post.getAuthor(), "Community post review updated", request.opinion(), "COMMUNITY_POST", post.getId());
    }

    private void auditComment(AuditDtos.AuditRequest request, User auditor) {
        CommunityComment comment = communityService.getManagedComment(request.targetId());
        if (request.action() == AuditAction.OFFLINE) {
            comment.setStatus(CommunityCommentStatus.OFFLINE);
        } else {
            if (comment.getStatus() != CommunityCommentStatus.PENDING_REVIEW) {
                throw new BusinessException("This community comment has already been reviewed");
            }
            comment.setStatus(request.action() == AuditAction.APPROVE ? CommunityCommentStatus.PUBLISHED : CommunityCommentStatus.REJECTED);
        }
        recordLog(AuditTargetType.COMMUNITY_COMMENT, comment.getId(), auditor, request.action(), request.opinion());
        notifyAuditResult(comment.getAuthor(), "Community comment review updated", request.opinion(), "COMMUNITY_COMMENT", comment.getId());
    }

    private void rejectOtherPendingApplies(AdoptApply approvedApply) {
        List<AdoptApply> pendingApplies = adoptApplyRepository.findByAnimalIdAndStatusAndIdNot(
                approvedApply.getAnimal().getId(),
                ApplyStatus.PENDING_REVIEW,
                approvedApply.getId()
        );
        for (AdoptApply pendingApply : pendingApplies) {
            pendingApply.setStatus(ApplyStatus.REJECTED);
            pendingApply.setAuditOpinion("Another adoption application has already been approved for this animal");
            notifyAuditResult(
                    pendingApply.getApplicant(),
                    "Adoption application review updated",
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
