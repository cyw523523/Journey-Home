package com.guitu.dto;

import com.guitu.domain.enums.CertificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RescueStationDtos {
    private RescueStationDtos() {
    }

    public record ApplyRequest(
            @NotBlank(message = "Station name is required")
            @Size(max = 120, message = "Station name must be at most 120 characters")
            String stationName,

            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            @Size(max = 255, message = "Address must be at most 255 characters")
            String address,

            @Size(max = 64, message = "Phone must be at most 64 characters")
            String contactPhone,

            @Size(max = 500, message = "Image URL must be at most 500 characters")
            String imageUrl
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Station name is required")
            @Size(max = 120)
            String stationName,

            @Size(max = 1000)
            String description,

            @Size(max = 255)
            String address,

            @Size(max = 64)
            String contactPhone,

            @Size(max = 500)
            String imageUrl
    ) {
    }

    public record StationResponse(
            Long id,
            Long userId,
            String nickname,
            String avatarUrl,
            String stationName,
            String description,
            String address,
            String contactPhone,
            String imageUrl,
            CertificationStatus certificationStatus,
            String certificationStatusText,
            Integer followerCount,
            String rejectReason,
            java.time.LocalDateTime createdAt
    ) {
    }

    public record DashboardResponse(
            Long stationId,
            String stationName,
            Integer rescueCount,
            Integer animalCount,
            Integer donationDemandCount,
            Integer totalDonationRecords,
            Integer volunteerTaskCount,
            Integer totalVolunteerApplications,
            Integer followerCount
    ) {
    }

    public record FollowResponse(
            Long id,
            Long userId,
            String nickname,
            String avatarUrl,
            String stationName,
            java.time.LocalDateTime followedAt
    ) {
    }

    public record CertifyRequest(
            @NotNull(message = "Status is required")
            CertificationStatus status,

            @Size(max = 500, message = "Reason must be at most 500 characters")
            String reason
    ) {
    }
}
