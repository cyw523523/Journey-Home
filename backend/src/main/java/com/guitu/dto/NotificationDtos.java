package com.guitu.dto;

import com.guitu.domain.enums.NotificationType;

import java.time.LocalDateTime;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String typeText,
            String title,
            String content,
            String relatedTargetType,
            Long relatedTargetId,
            boolean readFlag,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
    }

    public record NotificationSummary(
            long unreadCount
    ) {
    }
}
