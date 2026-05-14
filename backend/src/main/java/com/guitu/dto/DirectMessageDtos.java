package com.guitu.dto;

import com.guitu.domain.enums.UserRole;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class DirectMessageDtos {
    private DirectMessageDtos() {
    }

    public record UserSummary(
            Long id,
            String nickname,
            String avatarUrl,
            UserRole role,
            String roleText
    ) {
    }

    public record ConversationResponse(
            Long id,
            UserSummary peer,
            String lastMessageContent,
            Long lastSenderId,
            LocalDateTime lastMessageAt,
            long unreadCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MessageResponse(
            Long id,
            Long conversationId,
            UserSummary sender,
            UserSummary receiver,
            String content,
            List<String> imageUrls,
            boolean readFlag,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
    }

    public record ConversationDetailResponse(
            ConversationResponse conversation,
            List<MessageResponse> messages
    ) {
    }

    public record UnreadSummary(
            long unreadCount
    ) {
    }

    public record SendMessageRequest(
            @Size(max = 2000, message = "Message content must be at most 2000 characters")
            String content,
            List<@Size(max = 500, message = "Image URL must be at most 500 characters") String> imageUrls
    ) {
    }
}
