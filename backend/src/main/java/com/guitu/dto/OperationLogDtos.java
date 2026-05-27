package com.guitu.dto;

import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;

import java.time.LocalDateTime;

public final class OperationLogDtos {
    private OperationLogDtos() {
    }

    public record OperationLogResponse(
            Long id,
            OperationTargetType targetType,
            String targetTypeText,
            Long targetId,
            String targetName,
            OperationType operationType,
            String operationTypeText,
            Long operatorId,
            String operatorAccount,
            String operatorNickname,
            String operatorIp,
            String detail,
            String beforeSnapshot,
            String afterSnapshot,
            LocalDateTime operatedAt
    ) {
    }
}
