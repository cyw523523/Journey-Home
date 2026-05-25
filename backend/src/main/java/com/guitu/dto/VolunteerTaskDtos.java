package com.guitu.dto;

import com.guitu.domain.enums.ApplicationStatus;
import com.guitu.domain.enums.VolunteerTaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class VolunteerTaskDtos {
    private VolunteerTaskDtos() {
    }

    public record SaveVolunteerTaskRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 120, message = "Title must be at most 120 characters")
            String title,

            @NotBlank(message = "Description is required")
            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            @NotBlank(message = "Location is required")
            @Size(max = 255, message = "Location must be at most 255 characters")
            String location,

            @NotNull(message = "Max volunteers is required")
            @Min(value = 1, message = "Max volunteers must be at least 1")
            @Max(value = 100, message = "Max volunteers must be at most 100")
            Integer maxVolunteers,

            LocalDateTime scheduledTime,

            @Size(max = 500, message = "Image URL must be at most 500 characters")
            String imageUrl,

            Long relatedRescueId
    ) {
    }

    public record UpdateTaskStatusRequest(
            @NotNull(message = "Status is required")
            VolunteerTaskStatus status
    ) {
    }

    public record ReviewApplicationRequest(
            @NotNull(message = "Status is required")
            ApplicationStatus status,

            @Size(max = 500, message = "Review comment must be at most 500 characters")
            String reviewComment
    ) {
    }

    public record ApplyRequest(
            @Size(max = 500, message = "Message must be at most 500 characters")
            String message
    ) {
    }

    public record VolunteerTaskResponse(
            Long id,
            String title,
            String description,
            String location,
            Integer maxVolunteers,
            Integer currentVolunteers,
            LocalDateTime scheduledTime,
            String imageUrl,
            VolunteerTaskStatus status,
            String statusText,
            String reviewComment,
            Long publisherId,
            String publisherNickname,
            Long relatedRescueId,
            String relatedRescueLocation,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ApplicationResponse(
            Long id,
            Long taskId,
            String taskTitle,
            Long volunteerId,
            String volunteerNickname,
            String message,
            ApplicationStatus status,
            String statusText,
            String reviewComment,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {
    }
}
