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
            @NotBlank(message = "任务标题不能为空")
            @Size(max = 120, message = "任务标题长度不能超过120字符")
            String title,

            @NotBlank(message = "任务描述不能为空")
            @Size(max = 1000, message = "任务描述长度不能超过1000字符")
            String description,

            @NotBlank(message = "活动地点不能为空")
            @Size(max = 255, message = "活动地点长度不能超过255字符")
            String location,

            @NotNull(message = "招募人数不能为空")
            @Min(value = 1, message = "招募人数至少为1人")
            @Max(value = 100, message = "招募人数最多为100人")
            Integer maxVolunteers,

            LocalDateTime scheduledTime,

            @Size(max = 500, message = "封面图URL长度不能超过500字符")
            String imageUrl,

            Long relatedRescueId
    ) {
    }

    public record UpdateTaskStatusRequest(
            @NotNull(message = "状态不能为空")
            VolunteerTaskStatus status
    ) {
    }

    public record ReviewApplicationRequest(
            @NotNull(message = "状态不能为空")
            ApplicationStatus status,

            @Size(max = 500, message = "审核意见长度不能超过500字符")
            String reviewComment
    ) {
    }

    public record ApplyRequest(
            @Size(max = 500, message = "留言长度不能超过500字符")
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
