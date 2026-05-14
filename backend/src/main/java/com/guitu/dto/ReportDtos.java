package com.guitu.dto;

import com.guitu.domain.enums.ReportReasonType;
import com.guitu.domain.enums.ReportResolutionAction;
import com.guitu.domain.enums.ReportStatus;
import com.guitu.domain.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record CreateReportRequest(
            @NotNull(message = "Target type is required")
            ReportTargetType targetType,

            @NotNull(message = "Target id is required")
            Long targetId,

            @NotNull(message = "Reason is required")
            ReportReasonType reasonType,

            @NotBlank(message = "Description is required")
            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            List<@Size(max = 500, message = "Image URL must be at most 500 characters") String> evidenceImageUrls
    ) {
    }

    public record ResolveReportRequest(
            @NotNull(message = "Resolution action is required")
            ReportResolutionAction action,

            @NotBlank(message = "Resolution opinion is required")
            @Size(max = 500, message = "Resolution opinion must be at most 500 characters")
            String opinion
    ) {
    }

    public record ReportResponse(
            Long id,
            ReportTargetType targetType,
            String targetTypeText,
            Long targetId,
            Long reporterId,
            String reporterNickname,
            Long targetOwnerId,
            String targetOwnerNickname,
            ReportReasonType reasonType,
            String reasonTypeText,
            String description,
            List<String> evidenceImageUrls,
            ReportStatus status,
            String statusText,
            ReportResolutionAction resolutionAction,
            String resolutionActionText,
            String resolutionOpinion,
            Long reviewerId,
            String reviewerNickname,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            String targetContent
    ) {
    }
}
