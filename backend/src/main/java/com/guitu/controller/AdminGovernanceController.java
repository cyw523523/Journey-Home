package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.AppealStatus;
import com.guitu.domain.enums.AppealTargetType;
import com.guitu.domain.enums.ReportStatus;
import com.guitu.domain.enums.ReportTargetType;
import com.guitu.dto.AppealDtos;
import com.guitu.dto.ReportDtos;
import com.guitu.service.AppealService;
import com.guitu.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminGovernanceController {
    private final ReportService reportService;
    private final AppealService appealService;

    public AdminGovernanceController(ReportService reportService, AppealService appealService) {
        this.reportService = reportService;
        this.appealService = appealService;
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ReportDtos.ReportResponse>> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(reportService.adminList(status, targetType, page, size));
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<ReportDtos.ReportResponse> reportDetail(@PathVariable Long id) {
        return ApiResponse.ok(reportService.detail(id));
    }

    @PatchMapping("/reports/{id}")
    public ApiResponse<ReportDtos.ReportResponse> resolveReport(@PathVariable Long id, @Valid @RequestBody ReportDtos.ResolveReportRequest request) {
        return ApiResponse.ok(reportService.resolve(id, request));
    }

    @GetMapping("/appeals")
    public ApiResponse<PageResponse<AppealDtos.AppealResponse>> appeals(
            @RequestParam(required = false) AppealStatus status,
            @RequestParam(required = false) AppealTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(appealService.adminList(status, targetType, page, size));
    }

    @GetMapping("/appeals/{id}")
    public ApiResponse<AppealDtos.AppealResponse> appealDetail(@PathVariable Long id) {
        return ApiResponse.ok(appealService.detail(id));
    }

    @PatchMapping("/appeals/{id}")
    public ApiResponse<AppealDtos.AppealResponse> reviewAppeal(@PathVariable Long id, @Valid @RequestBody AppealDtos.ReviewAppealRequest request) {
        return ApiResponse.ok(appealService.review(id, request));
    }
}
