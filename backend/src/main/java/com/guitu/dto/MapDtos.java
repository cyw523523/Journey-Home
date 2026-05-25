package com.guitu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 地图展示模块 DTO。
 */
public final class MapDtos {
    private MapDtos() {
    }

    public record Coordinate(
            @NotNull(message = "经度不能为空")
            @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
            @DecimalMax(value = "180.0", message = "经度不能大于 180")
            Double longitude,

            @NotNull(message = "纬度不能为空")
            @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
            @DecimalMax(value = "90.0", message = "纬度不能大于 90")
            Double latitude
    ) {
    }

    public record SaveShelterStationRequest(
            @NotBlank(message = "救助站名称不能为空")
            @Size(max = 120, message = "救助站名称长度不能超过120")
            String name,

            @NotBlank(message = "救助站地址不能为空")
            @Size(max = 255, message = "救助站地址长度不能超过255")
            String address,

            @NotNull(message = "经度不能为空")
            @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
            @DecimalMax(value = "180.0", message = "经度不能大于 180")
            Double longitude,

            @NotNull(message = "纬度不能为空")
            @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
            @DecimalMax(value = "90.0", message = "纬度不能大于 90")
            Double latitude,

            @Size(max = 64, message = "联系电话长度不能超过64")
            String contactPhone,

            @Size(max = 128, message = "服务时间长度不能超过128")
            String serviceTime,

            @Size(max = 1000, message = "说明长度不能超过1000")
            String description,

            Boolean enabled
    ) {
    }

    public record ShelterStationResponse(
            Long id,
            String name,
            String address,
            Double longitude,
            Double latitude,
            String contactPhone,
            String serviceTime,
            String description,
            Boolean enabled,
            Long createdById,
            String createdByNickname,
            Double distanceKm,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MapPointResponse(
            Long id,
            String pointType,
            String title,
            String address,
            Double longitude,
            Double latitude,
            String description,
            String coverImageUrl,
            String statusText,
            Double distanceKm,
            String link,
            LocalDateTime createdAt
    ) {
    }

    public record NearbyResponse(
            List<MapPointResponse> animals,
            List<MapPointResponse> stations
    ) {
    }
}
