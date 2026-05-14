package com.guitu.domain;

import com.guitu.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_notifications")
public class SystemNotification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type = NotificationType.SYSTEM;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 64)
    private String relatedTargetType;

    private Long relatedTargetId;

    @Column(nullable = false)
    private boolean readFlag = false;

    private LocalDateTime readAt;
}
