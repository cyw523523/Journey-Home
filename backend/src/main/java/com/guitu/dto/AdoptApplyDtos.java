package com.guitu.dto;

import com.guitu.domain.enums.ApplyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

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
}
