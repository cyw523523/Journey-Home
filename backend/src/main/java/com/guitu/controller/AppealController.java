package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.dto.AppealDtos;
import com.guitu.service.AppealService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appeals")
public class AppealController {
    private final AppealService appealService;

    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    @PostMapping
    public ApiResponse<AppealDtos.AppealResponse> create(@Valid @RequestBody AppealDtos.CreateAppealRequest request) {
        return ApiResponse.ok(appealService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<AppealDtos.AppealResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(appealService.listMine(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppealDtos.AppealResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(appealService.detail(id));
    }
}
