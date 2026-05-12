package com.guitu.dto;

import com.guitu.domain.enums.AuditAction;
import com.guitu.domain.enums.AuditTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditRequest(
            @NotNull(message = "审核对象类型不能为空")
            AuditTargetType targetType,

            @NotNull(message = "审核对象编号不能为空")
            Long targetId,

            @NotNull(message = "审核结果不能为空")
            AuditAction action,

            @NotBlank(message = "审核意见不能为空")
            @Size(max = 500, message = "审核意见长度不能超过500")
            String opinion
    ) {
    }

    public record AuditDecisionRequest(
            @NotNull(message = "审核结果不能为空")
            AuditAction action,

            @NotBlank(message = "审核意见不能为空")
            @Size(max = 500, message = "审核意见长度不能超过500")
            String opinion
    ) {
    }

    public record PendingItemResponse(
            AuditTargetType targetType,
            Long targetId,
            String title,
            String status,
            String publisherOrApplicant,
            LocalDateTime createdAt
    ) {
    }

    public record AuditLogResponse(
            Long id,
            AuditTargetType targetType,
            String targetTypeText,
            Long targetId,
            Long auditorId,
            String auditorNickname,
            AuditAction action,
            String actionText,
            String opinion,
            LocalDateTime auditTime
    ) {
    }
}
