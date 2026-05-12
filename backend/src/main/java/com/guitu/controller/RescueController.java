package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.dto.RescueDtos;
import com.guitu.service.RescueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescues")
public class RescueController {
    private final RescueService rescueService;

    public RescueController(RescueService rescueService) {
        this.rescueService = rescueService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RescueDtos.RescueResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) RescueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(rescueService.listPublic(keyword, region, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<RescueDtos.RescueResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(rescueService.detail(id));
    }

    @PostMapping
    public ApiResponse<RescueDtos.RescueResponse> create(@Valid @RequestBody RescueDtos.SaveRescueRequest request) {
        return ApiResponse.ok(rescueService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RescueDtos.RescueResponse> update(@PathVariable Long id, @Valid @RequestBody RescueDtos.SaveRescueRequest request) {
        return ApiResponse.ok(rescueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        rescueService.offline(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RescueDtos.RescueResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RescueDtos.UpdateRescueStatusRequest request
    ) {
        return ApiResponse.ok(rescueService.updateStatus(id, request));
    }
}
