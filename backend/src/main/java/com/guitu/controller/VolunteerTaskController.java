package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.ApplicationStatus;
import com.guitu.domain.enums.VolunteerTaskStatus;
import com.guitu.dto.VolunteerTaskDtos;
import com.guitu.service.VolunteerTaskService;
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

import java.util.List;

@RestController
@RequestMapping("/api/volunteer-tasks")
public class VolunteerTaskController {
    private final VolunteerTaskService volunteerTaskService;

    public VolunteerTaskController(VolunteerTaskService volunteerTaskService) {
        this.volunteerTaskService = volunteerTaskService;
    }

    @GetMapping
    public ApiResponse<PageResponse<VolunteerTaskDtos.VolunteerTaskResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) VolunteerTaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(volunteerTaskService.listPublic(keyword, region, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<VolunteerTaskDtos.VolunteerTaskResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(volunteerTaskService.detail(id));
    }

    @GetMapping("/{id}/applications")
    public ApiResponse<List<VolunteerTaskDtos.ApplicationResponse>> applications(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(volunteerTaskService.listApplications(id, page, size));
    }

    @PostMapping
    public ApiResponse<VolunteerTaskDtos.VolunteerTaskResponse> create(@Valid @RequestBody VolunteerTaskDtos.SaveVolunteerTaskRequest request) {
        return ApiResponse.ok(volunteerTaskService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VolunteerTaskDtos.VolunteerTaskResponse> update(@PathVariable Long id, @Valid @RequestBody VolunteerTaskDtos.SaveVolunteerTaskRequest request) {
        return ApiResponse.ok(volunteerTaskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        volunteerTaskService.offline(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<VolunteerTaskDtos.VolunteerTaskResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody VolunteerTaskDtos.UpdateTaskStatusRequest request
    ) {
        return ApiResponse.ok(volunteerTaskService.updateStatus(id, request));
    }

    @PostMapping("/{id}/apply")
    public ApiResponse<VolunteerTaskDtos.ApplicationResponse> apply(@PathVariable Long id, @Valid @RequestBody VolunteerTaskDtos.ApplyRequest request) {
        return ApiResponse.ok(volunteerTaskService.apply(id, request));
    }

    @PatchMapping("/applications/{applicationId}/review")
    public ApiResponse<VolunteerTaskDtos.ApplicationResponse> reviewApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody VolunteerTaskDtos.ReviewApplicationRequest request
    ) {
        return ApiResponse.ok(volunteerTaskService.reviewApplication(applicationId, request));
    }

    @PatchMapping("/applications/{applicationId}/complete")
    public ApiResponse<VolunteerTaskDtos.ApplicationResponse> completeApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(volunteerTaskService.completeApplication(applicationId));
    }
}
