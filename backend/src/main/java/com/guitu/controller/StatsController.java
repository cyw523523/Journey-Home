package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.dto.StatsDtos;
import com.guitu.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StatsController {
    private final StatisticsService statisticsService;

    public StatsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/api/home/overview")
    public ApiResponse<StatsDtos.HomeOverviewResponse> homeOverview() {
        return ApiResponse.ok(statisticsService.homeOverview());
    }

    @GetMapping("/api/admin/stats/overview")
    public ApiResponse<StatsDtos.OverviewResponse> overview() {
        return ApiResponse.ok(statisticsService.overview());
    }

    @GetMapping("/api/admin/stats/animals/status")
    public ApiResponse<List<StatsDtos.StatusCount>> animalStatus() {
        return ApiResponse.ok(statisticsService.animalStatusDistribution());
    }

    @GetMapping("/api/admin/stats/rescues/status")
    public ApiResponse<List<StatsDtos.StatusCount>> rescueStatus() {
        return ApiResponse.ok(statisticsService.rescueStatusDistribution());
    }

    @GetMapping("/api/admin/stats/applications/status")
    public ApiResponse<List<StatsDtos.StatusCount>> applyStatus() {
        return ApiResponse.ok(statisticsService.applyStatusDistribution());
    }
}
