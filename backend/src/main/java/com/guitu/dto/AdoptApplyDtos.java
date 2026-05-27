package com.guitu.dto;

import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.AdoptionAgreementStatus;
import com.guitu.domain.enums.AdoptionFollowUpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AdoptApplyDtos {
    private AdoptApplyDtos() {
    }

    public record CreateApplyRequest(
            @NotNull(message = "Animal id is required")
            Long animalId,

            @NotBlank(message = "Applicant name is required")
            @Size(max = 64, message = "Applicant name must be at most 64 characters")
            String applicantName,

            @NotBlank(message = "Contact is required")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Contact format is invalid")
            @Size(max = 64, message = "Contact must be at most 64 characters")
            String contact,

            @NotBlank(message = "Reason is required")
            @Size(max = 1000, message = "Reason must be at most 1000 characters")
            String reason,

            @NotBlank(message = "Living condition is required")
            @Size(max = 1000, message = "Living condition must be at most 1000 characters")
            String livingCondition,

            @NotBlank(message = "Experience is required")
            @Size(max = 1000, message = "Experience must be at most 1000 characters")
            String experience
    ) {
    }

    public record ApplyResponse(
            Long id,
            Long animalId,
            String animalTypeText,
            Long publisherId,
            String publisherNickname,
            Long applicantId,
            String applicantName,
            String contact,
            String reason,
            String livingCondition,
            String experience,
            ApplyStatus status,
            String statusText,
            String auditOpinion,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SignAgreementRequest(
            @Size(max = 64, message = "签名长度不能超过64个字符")
            String signatureName,
            String signatureDataUrl
    ) {
    }

    public record AgreementResponse(
            Long id,
            Long applyId,
            String agreementNo,
            String title,
            String content,
            AdoptionAgreementStatus status,
            String statusText,
            Long adopterId,
            String adopterNickname,
            Long publisherId,
            String publisherNickname,
            String adopterSignatureName,
            String adopterSignatureImageUrl,
            LocalDateTime adopterSignedAt,
            String counterpartSignatureName,
            String counterpartSignatureImageUrl,
            LocalDateTime counterpartSignedAt,
            String pdfUrl,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CompleteFollowUpRequest(
            @NotBlank(message = "回访内容不能为空")
            @Size(max = 1000, message = "回访内容不能超过1000个字符")
            String note,

            List<String> imageUrls
    ) {
    }

    public record FollowUpResponse(
            Long id,
            Long applyId,
            String stageCode,
            String stageLabel,
            LocalDateTime plannedAt,
            LocalDateTime completedAt,
            AdoptionFollowUpStatus status,
            String statusText,
            String note,
            List<String> imageUrls,
            Long creatorId,
            String creatorNickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
