package com.guitu.dto;

import com.guitu.domain.enums.AppealDecisionAction;
import com.guitu.domain.enums.AppealStatus;
import com.guitu.domain.enums.AppealTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AppealDtos {
    private AppealDtos() {
    }

    public record CreateAppealRequest(
            @NotNull(message = "Target type is required")
            AppealTargetType targetType,

            @NotNull(message = "Target id is required")
            Long targetId,

            @NotBlank(message = "Appeal reason is required")
            @Size(max = 1000, message = "Appeal reason must be at most 1000 characters")
            String reason
    ) {
    }

    public record ReviewAppealRequest(
            @NotNull(message = "Review action is required")
            AppealDecisionAction action,

            @NotBlank(message = "Review opinion is required")
            @Size(max = 500, message = "Review opinion must be at most 500 characters")
            String opinion
    ) {
    }

    public record AppealResponse(
            Long id,
            Long applicantId,
            String applicantNickname,
            AppealTargetType targetType,
            String targetTypeText,
            Long targetId,
            String reason,
            AppealStatus status,
            String statusText,
            Long firstReviewerId,
            String firstReviewerNickname,
            String firstReviewOpinion,
            Long secondReviewerId,
            String secondReviewerNickname,
            String finalReviewOpinion,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt
    ) {
    }
}
