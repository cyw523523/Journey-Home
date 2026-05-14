package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.dto.ReportDtos;
import com.guitu.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<ReportDtos.ReportResponse> create(@Valid @RequestBody ReportDtos.CreateReportRequest request) {
        return ApiResponse.ok(reportService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReportDtos.ReportResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(reportService.listMine(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportDtos.ReportResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(reportService.detail(id));
    }
}
