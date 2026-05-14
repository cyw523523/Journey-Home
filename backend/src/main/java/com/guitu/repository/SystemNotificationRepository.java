package com.guitu.repository;

import com.guitu.domain.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    Page<SystemNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFlagFalse(Long recipientId);
}
