package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.AuditLog;
import com.guitu.domain.Rescue;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.AuditAction;
import com.guitu.domain.enums.AuditTargetType;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.dto.AuditDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AdoptApplyRepository;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.AuditLogRepository;
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
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final AdoptApplyService adoptApplyService;
    private final DtoMapper mapper;

    public AuditService(
            AnimalRepository animalRepository,
            RescueRepository rescueRepository,
            AdoptApplyRepository adoptApplyRepository,
            AuditLogRepository auditLogRepository,
            UserService userService,
            AnimalService animalService,
            RescueService rescueService,
            AdoptApplyService adoptApplyService,
            DtoMapper mapper
    ) {
        this.animalRepository = animalRepository;
        this.rescueRepository = rescueRepository;
        this.adoptApplyRepository = adoptApplyRepository;
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.adoptApplyService = adoptApplyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.PendingItemResponse> pending(AuditTargetType targetType, int page, int size) {
        if (targetType == AuditTargetType.ANIMAL) {
            return animalRepository.findAll(animalPendingSpec(), pageRequest(page, size)).getContent()
                    .stream().map(this::pendingAnimal).toList();
        }
        if (targetType == AuditTargetType.RESCUE) {
            return rescueRepository.findAll(rescuePendingSpec(), pageRequest(page, size)).getContent()
                    .stream().map(this::pendingRescue).toList();
        }
        if (targetType == AuditTargetType.ADOPT_APPLY) {
            return adoptApplyRepository.findAll(applyPendingSpec(), pageRequest(page, size)).getContent()
                    .stream().map(this::pendingApply).toList();
        }

        List<AuditDtos.PendingItemResponse> result = new ArrayList<>();
        result.addAll(animalRepository.findAll(animalPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingAnimal).toList());
        result.addAll(rescueRepository.findAll(rescuePendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingRescue).toList());
        result.addAll(adoptApplyRepository.findAll(applyPendingSpec(), pageRequest(0, size)).getContent().stream().map(this::pendingApply).toList());
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
        };
    }

    @Transactional
    public void audit(AuditDtos.AuditRequest request) {
        User auditor = userService.currentUser();
        switch (request.targetType()) {
            case ANIMAL -> auditAnimal(request, auditor);
            case RESCUE -> auditRescue(request, auditor);
            case ADOPT_APPLY -> auditApply(request, auditor);
        }
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
                throw new BusinessException("该数据已完成审核");
            }
            if (request.action() == AuditAction.APPROVE) {
                animal.setStatus(AnimalStatus.WAITING_ADOPTION);
            } else {
                animal.setStatus(AnimalStatus.REJECTED);
            }
        }
        animal.setReviewComment(request.opinion());
        recordLog(AuditTargetType.ANIMAL, animal.getId(), auditor, request.action(), request.opinion());
    }

    private void auditRescue(AuditDtos.AuditRequest request, User auditor) {
        Rescue rescue = rescueService.getEntity(request.targetId());
        if (request.action() == AuditAction.OFFLINE) {
            rescue.setStatus(RescueStatus.OFFLINE);
        } else {
            if (rescue.getStatus() != RescueStatus.PENDING_REVIEW) {
                throw new BusinessException("该数据已完成审核");
            }
            if (request.action() == AuditAction.APPROVE) {
                rescue.setStatus(RescueStatus.PENDING_PROCESS);
            } else {
                rescue.setStatus(RescueStatus.REJECTED);
            }
        }
        rescue.setReviewComment(request.opinion());
        recordLog(AuditTargetType.RESCUE, rescue.getId(), auditor, request.action(), request.opinion());
    }

    private void auditApply(AuditDtos.AuditRequest request, User auditor) {
        if (request.action() == AuditAction.OFFLINE) {
            throw new BusinessException("领养申请不支持下架操作");
        }
        AdoptApply apply = adoptApplyService.getEntity(request.targetId());
        if (apply.getStatus() != ApplyStatus.PENDING_REVIEW) {
            throw new BusinessException("该数据已完成审核");
        }
        if (request.action() == AuditAction.APPROVE) {
            if (apply.getAnimal().getStatus() != AnimalStatus.WAITING_ADOPTION) {
                throw new BusinessException("该动物当前不可申请领养");
            }
            apply.setStatus(ApplyStatus.APPROVED);
            apply.getAnimal().setStatus(AnimalStatus.ADOPTED);
        } else {
            apply.setStatus(ApplyStatus.REJECTED);
        }
        apply.setAuditOpinion(request.opinion());
        recordLog(AuditTargetType.ADOPT_APPLY, apply.getId(), auditor, request.action(), request.opinion());
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
        return new AuditDtos.PendingItemResponse(
                AuditTargetType.ANIMAL,
                animal.getId(),
                animal.getType().getLabel() + " / " + animal.getFoundRegion(),
                animal.getStatus().getLabel(),
                animal.getPublisher().getNickname(),
                animal.getCreatedAt()
        );
    }

    private AuditDtos.PendingItemResponse pendingRescue(Rescue rescue) {
        return new AuditDtos.PendingItemResponse(
                AuditTargetType.RESCUE,
                rescue.getId(),
                rescue.getLocation() + " / " + rescue.getAnimalCondition(),
                rescue.getStatus().getLabel(),
                rescue.getPublisher().getNickname(),
                rescue.getCreatedAt()
        );
    }

    private AuditDtos.PendingItemResponse pendingApply(AdoptApply apply) {
        return new AuditDtos.PendingItemResponse(
                AuditTargetType.ADOPT_APPLY,
                apply.getId(),
                "领养申请 #" + apply.getId(),
                apply.getStatus().getLabel(),
                apply.getApplicantName(),
                apply.getCreatedAt()
        );
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

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
