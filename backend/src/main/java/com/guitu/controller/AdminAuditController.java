package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.AuditTargetType;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.dto.AuditDtos;
import com.guitu.service.AdoptApplyService;
import com.guitu.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditController {
    private final AuditService auditService;
    private final AdoptApplyService adoptApplyService;

    public AdminAuditController(AuditService auditService, AdoptApplyService adoptApplyService) {
        this.auditService = auditService;
        this.adoptApplyService = adoptApplyService;
    }

    @GetMapping("/audits/pending")
    public ApiResponse<List<AuditDtos.PendingItemResponse>> pending(
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(auditService.pending(targetType, page, size));
    }

    @GetMapping("/audits/{targetType}/{targetId}")
    public ApiResponse<Object> auditDetail(@PathVariable AuditTargetType targetType, @PathVariable Long targetId) {
        return ApiResponse.ok(auditService.detail(targetType, targetId));
    }

    @PostMapping("/audits")
    public ApiResponse<Void> audit(@Valid @RequestBody AuditDtos.AuditRequest request) {
        auditService.audit(request);
        return ApiResponse.ok();
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditDtos.AuditLogResponse>> logs(
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(auditService.logs(targetType, targetId, page, size));
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<AdoptApplyDtos.ApplyResponse>> applications(
            @RequestParam(required = false) ApplyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(adoptApplyService.adminList(status, page, size));
    }

    @PatchMapping("/applications/{id}/audit")
    public ApiResponse<Void> auditApplication(@PathVariable Long id, @Valid @RequestBody AuditDtos.AuditDecisionRequest request) {
        auditService.audit(new AuditDtos.AuditRequest(AuditTargetType.ADOPT_APPLY, id, request.action(), request.opinion()));
        return ApiResponse.ok();
    }
}
