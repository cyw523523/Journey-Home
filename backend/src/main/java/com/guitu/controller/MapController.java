package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.MapDtos;
import com.guitu.service.MapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {
    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/points")
    public ApiResponse<List<MapDtos.MapPointResponse>> points(
            @RequestParam(defaultValue = "ALL") String pointType,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "100") Integer limit
    ) {
        return ApiResponse.ok(mapService.listPoints(pointType, latitude, longitude, radiusKm, limit));
    }

    @GetMapping("/animals/nearby")
    public ApiResponse<List<MapDtos.MapPointResponse>> nearbyAnimals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radiusKm,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(mapService.nearbyAnimals(latitude, longitude, radiusKm, limit));
    }

    @GetMapping("/stations")
    public ApiResponse<List<MapDtos.ShelterStationResponse>> stations(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm
    ) {
        return ApiResponse.ok(mapService.listPublicStations(latitude, longitude, radiusKm));
    }

    @GetMapping("/stations/nearby")
    public ApiResponse<List<MapDtos.MapPointResponse>> nearbyStations(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radiusKm,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(mapService.nearbyStations(latitude, longitude, radiusKm, limit));
    }

    @GetMapping("/around")
    public ApiResponse<MapDtos.NearbyResponse> around(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radiusKm,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(mapService.around(latitude, longitude, radiusKm, limit));
    }
}
