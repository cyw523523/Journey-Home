package com.guitu.dto;

import com.guitu.domain.enums.RescueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class RescueDtos {
    private RescueDtos() {
    }

    public record SaveRescueRequest(
            @NotBlank(message = "Location is required")
            @Size(max = 255, message = "Location must be at most 255 characters")
            String location,

            @NotBlank(message = "Animal condition is required")
            @Size(max = 500, message = "Animal condition must be at most 500 characters")
            String animalCondition,

            @NotBlank(message = "Contact is required")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Contact format is invalid")
            @Size(max = 64, message = "Contact must be at most 64 characters")
            String contact,

            @NotBlank(message = "Description is required")
            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            List<@Size(max = 500, message = "Image URL must be at most 500 characters") String> imageUrls
    ) {
    }

    public record UpdateRescueStatusRequest(
            @NotNull(message = "Status is required")
            RescueStatus status
    ) {
    }

    public record RescueResponse(
            Long id,
            String location,
            String animalCondition,
            String contact,
            String description,
            List<String> imageUrls,
            RescueStatus status,
            String statusText,
            String reviewComment,
            Long publisherId,
            String publisherNickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
