package com.guitu.repository;

import com.guitu.domain.OperationLog;
import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {
    boolean existsByTargetTypeAndTargetIdAndOperationTypeAndOperatorIdAndOperatedAt(
            OperationTargetType targetType,
            Long targetId,
            OperationType operationType,
            Long operatorId,
            LocalDateTime operatedAt
    );
}
