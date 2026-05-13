package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.SmartAdoptionDtos;
import com.guitu.service.AdoptApplyService;
import com.guitu.service.SmartAdoptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adoptions")
public class AdoptApplyController {
    private final AdoptApplyService adoptApplyService;
    private final SmartAdoptionService smartAdoptionService;

    public AdoptApplyController(AdoptApplyService adoptApplyService, SmartAdoptionService smartAdoptionService) {
        this.adoptApplyService = adoptApplyService;
        this.smartAdoptionService = smartAdoptionService;
    }

    @PostMapping
    public ApiResponse<AdoptApplyDtos.ApplyResponse> create(@Valid @RequestBody AdoptApplyDtos.CreateApplyRequest request) {
        return ApiResponse.ok(adoptApplyService.create(request));
    }

    @PostMapping("/smart-match")
    public ApiResponse<SmartAdoptionDtos.SmartMatchResponse> smartMatch(
            @Valid @RequestBody SmartAdoptionDtos.SmartMatchRequest request
    ) {
        return ApiResponse.ok(smartAdoptionService.generateAdvice(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<AdoptApplyDtos.ApplyResponse>> listMine(
            @RequestParam(required = false) ApplyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(adoptApplyService.listMine(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdoptApplyDtos.ApplyResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adoptApplyService.detail(id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        adoptApplyService.cancel(id);
        return ApiResponse.ok();
    }
}
