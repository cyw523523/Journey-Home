package com.guitu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitu.common.PageResponse;
import com.guitu.domain.AuditLog;
import com.guitu.domain.OperationLog;
import com.guitu.domain.User;
import com.guitu.domain.enums.AuditAction;
import com.guitu.domain.enums.AuditTargetType;
import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;
import com.guitu.dto.OperationLogDtos;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AuditLogRepository;
import com.guitu.repository.OperationLogRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OperationLogService {
    private final OperationLogRepository operationLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(
            OperationLogRepository operationLogRepository,
            AuditLogRepository auditLogRepository,
            UserService userService,
            DtoMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.operationLogRepository = operationLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(
            OperationTargetType targetType,
            Long targetId,
            String targetName,
            OperationType operationType,
            String detail,
            Object beforeSnapshot,
            Object afterSnapshot
    ) {
        record(userService.currentUser(), targetType, targetId, targetName, operationType, detail, beforeSnapshot, afterSnapshot);
    }

    @Transactional
    public void record(
            User operator,
            OperationTargetType targetType,
            Long targetId,
            String targetName,
            OperationType operationType,
            String detail,
            Object beforeSnapshot,
            Object afterSnapshot
    ) {
        OperationLog log = new OperationLog();
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName == null || targetName.isBlank() ? targetType.getLabel() + "#" + targetId : targetName);
        log.setOperator(operator);
        log.setOperationType(operationType);
        log.setOperatorIp(resolveIpAddress());
        log.setDetail(detail);
        log.setBeforeSnapshot(serialize(beforeSnapshot));
        log.setAfterSnapshot(serialize(afterSnapshot));
        log.setOperatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    @Transactional
    public PageResponse<OperationLogDtos.OperationLogResponse> list(
            OperationTargetType targetType,
            OperationType operationType,
            String operatorKeyword,
            String keyword,
            LocalDateTime startAt,
            LocalDateTime endAt,
            int page,
            int size
    ) {
        backfillHistoricalAuditLogsIfNeeded();
        Page<OperationLog> result = operationLogRepository.findAll(spec(targetType, operationType, operatorKeyword, keyword, startAt, endAt), pageRequest(page, size));
        return PageResponse.from(result, mapper::toOperationLogResponse);
    }

    private void backfillHistoricalAuditLogsIfNeeded() {
        if (operationLogRepository.count() > 0 || auditLogRepository.count() == 0) {
            return;
        }

        List<AuditLog> auditLogs = auditLogRepository.findAll(Sort.by(Sort.Direction.ASC, "auditTime"));
        for (AuditLog auditLog : auditLogs) {
            OperationLog log = new OperationLog();
            log.setTargetType(mapTargetType(auditLog.getTargetType()));
            log.setTargetId(auditLog.getTargetId());
            log.setTargetName(auditLog.getTargetType().getLabel() + " #" + auditLog.getTargetId());
            log.setOperator(auditLog.getAuditor());
            log.setOperationType(mapOperationType(auditLog.getAction(), auditLog.getTargetType()));
            log.setOperatorIp("HISTORY");
            log.setDetail("历史审核记录回填");
            log.setBeforeSnapshot(null);
            log.setAfterSnapshot(serialize(java.util.Map.of(
                    "source", "audit_logs",
                    "auditLogId", auditLog.getId(),
                    "action", auditLog.getAction().name(),
                    "opinion", auditLog.getOpinion(),
                    "auditTime", auditLog.getAuditTime()
            )));
            log.setOperatedAt(auditLog.getAuditTime());
            operationLogRepository.save(log);
        }
    }

    private OperationTargetType mapTargetType(AuditTargetType targetType) {
        return switch (targetType) {
            case ANIMAL -> OperationTargetType.ANIMAL;
            case RESCUE -> OperationTargetType.RESCUE;
            case ADOPT_APPLY -> OperationTargetType.ADOPT_APPLY;
            case COMMUNITY_POST -> OperationTargetType.COMMUNITY_POST;
            case COMMUNITY_COMMENT -> OperationTargetType.COMMUNITY_COMMENT;
        };
    }

    private OperationType mapOperationType(AuditAction action, AuditTargetType targetType) {
        if (action == AuditAction.OFFLINE) {
            return OperationType.OFFLINE;
        }
        if (targetType == AuditTargetType.ADOPT_APPLY) {
            return action == AuditAction.APPROVE ? OperationType.APPROVE_APPLICATION : OperationType.REJECT_APPLICATION;
        }
        return OperationType.STATUS_CHANGE;
    }

    private Specification<OperationLog> spec(
            OperationTargetType targetType,
            OperationType operationType,
            String operatorKeyword,
            String keyword,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            root.join("operator", JoinType.LEFT);
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (operationType != null) {
                predicates.add(cb.equal(root.get("operationType"), operationType));
            }
            if (operatorKeyword != null && !operatorKeyword.isBlank()) {
                String like = "%" + operatorKeyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("operator").get("nickname"), like),
                        cb.like(root.get("operator").get("account"), like)
                ));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("targetName"), like),
                        cb.like(root.get("detail"), like)
                ));
            }
            if (startAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operatedAt"), startAt));
            }
            if (endAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operatedAt"), endAt));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "operatedAt"));
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String resolveIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return "SYSTEM";
        }
        String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = attrs.getRequest().getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return attrs.getRequest().getRemoteAddr();
    }
}
