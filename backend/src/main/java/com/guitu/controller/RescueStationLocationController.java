package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.LocationDtos;
import com.guitu.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 救助站位置相关接口。
 */
@RestController
@RequestMapping({"/api/rescue-station", "/api/rescue-stations"})
public class RescueStationLocationController {
    private final LocationService locationService;

    public RescueStationLocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 管理员更新救助站位置。
     */
    @PostMapping("/location")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocationDtos.RescueStationLocationResponse> saveLocationByPost(
            @Valid @RequestBody LocationDtos.RescueStationLocationRequest request
    ) {
        return ApiResponse.ok(locationService.updateRescueStationLocation(request));
    }

    /**
     * 管理员更新救助站位置。
     */
    @PutMapping("/location")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocationDtos.RescueStationLocationResponse> saveLocationByPut(
            @Valid @RequestBody LocationDtos.RescueStationLocationRequest request
    ) {
        return ApiResponse.ok(locationService.updateRescueStationLocation(request));
    }

    /**
     * 按距离筛选救助站。
     */
    @GetMapping("/nearby")
    public ApiResponse<LocationDtos.NearbyRescueStationListResponse> nearbyStations(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double distance
    ) {
        return ApiResponse.ok(locationService.nearbyRescueStations(latitude, longitude, distance));
    }
}
