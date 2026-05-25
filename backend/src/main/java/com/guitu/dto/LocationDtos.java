package com.guitu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 地理位置模块 DTO。
 */
public final class LocationDtos {
    private LocationDtos() {
    }

    public record AnimalLocationRequest(
            @NotNull(message = "动物ID不能为空")
            Long animalId,

            @NotNull(message = "纬度不能为空")
            @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
            @DecimalMax(value = "90.0", message = "纬度不能大于 90")
            Double latitude,

            @NotNull(message = "经度不能为空")
            @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
            @DecimalMax(value = "180.0", message = "经度不能大于 180")
            Double longitude
    ) {
    }

    public record RescueStationLocationRequest(
            @NotNull(message = "救助站ID不能为空")
            Long stationId,

            @NotNull(message = "纬度不能为空")
            @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
            @DecimalMax(value = "90.0", message = "纬度不能大于 90")
            Double latitude,

            @NotNull(message = "经度不能为空")
            @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
            @DecimalMax(value = "180.0", message = "经度不能大于 180")
            Double longitude,

            @Size(max = 255, message = "详细地址长度不能超过255")
            String addressDetail
    ) {
    }

    public record AnimalLocationResponse(
            Long animalId,
            Double latitude,
            Double longitude,
            LocalDateTime updatedAt
    ) {
    }

    public record RescueStationLocationResponse(
            Long stationId,
            String stationName,
            Double latitude,
            Double longitude,
            String addressDetail,
            LocalDateTime updatedAt
    ) {
    }

    public record NearbyAnimalResponse(
            Long animalId,
            String typeText,
            String genderText,
            Integer age,
            String foundRegion,
            Double latitude,
            Double longitude,
            String statusText,
            String coverImageUrl,
            Double distanceKm,
            LocalDateTime createdAt
    ) {
    }

    public record NearbyRescueStationResponse(
            Long stationId,
            String stationName,
            String addressDetail,
            Double latitude,
            Double longitude,
            String contactPhone,
            String serviceTime,
            Double distanceKm,
            LocalDateTime updatedAt
    ) {
    }

    public record NearbyAnimalListResponse(
            Double userLatitude,
            Double userLongitude,
            Double distance,
            Integer count,
            List<NearbyAnimalResponse> list
    ) {
    }

    public record NearbyRescueStationListResponse(
            Double userLatitude,
            Double userLongitude,
            Double distance,
            Integer count,
            List<NearbyRescueStationResponse> list
    ) {
    }
}
