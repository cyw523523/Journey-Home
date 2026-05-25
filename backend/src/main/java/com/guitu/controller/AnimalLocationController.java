package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.LocationDtos;
import com.guitu.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动物位置相关接口。
 */
@RestController
@RequestMapping({"/api/animal", "/api/animals"})
public class AnimalLocationController {
    private final LocationService locationService;

    public AnimalLocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 位置上传接口。
     */
    @PostMapping("/location")
    public ApiResponse<LocationDtos.AnimalLocationResponse> uploadLocation(
            @Valid @RequestBody LocationDtos.AnimalLocationRequest request
    ) {
        return ApiResponse.ok(locationService.updateAnimalLocation(request));
    }

    /**
     * 按距离筛选动物信息。
     */
    @GetMapping("/nearby")
    public ApiResponse<LocationDtos.NearbyAnimalListResponse> nearbyAnimals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double distance
    ) {
        return ApiResponse.ok(locationService.nearbyAnimals(latitude, longitude, distance));
    }
}
