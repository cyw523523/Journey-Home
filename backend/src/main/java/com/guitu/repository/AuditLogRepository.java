package com.guitu.repository;

import com.guitu.domain.AuditLog;
import com.guitu.domain.enums.AuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByTargetTypeAndTargetId(AuditTargetType targetType, Long targetId, Pageable pageable);
}
