package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.dto.NotificationDtos;
import com.guitu.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationDtos.NotificationResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(notificationService.mine(page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<NotificationDtos.NotificationSummary> summary() {
        return ApiResponse.ok(notificationService.summary());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok();
    }
}
