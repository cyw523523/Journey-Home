package com.guitu.dto;

import com.guitu.domain.enums.CertificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RescueStationDtos {
    private RescueStationDtos() {
    }

    public record ApplyRequest(
            @NotBlank(message = "救助站名称不能为空")
            @Size(max = 120, message = "救助站名称长度不能超过120字符")
            String stationName,

            @Size(max = 1000, message = "描述长度不能超过1000字符")
            String description,

            @Size(max = 255, message = "地址长度不能超过255字符")
            String address,

            @Size(max = 64, message = "联系电话长度不能超过64字符")
            String contactPhone,

            @Size(max = 500, message = "图片URL长度不能超过500字符")
            String imageUrl
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "救助站名称不能为空")
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
            @NotNull(message = "状态不能为空")
            CertificationStatus status,

            @Size(max = 500, message = "原因说明长度不能超过500字符")
            String reason
    ) {
    }
}
