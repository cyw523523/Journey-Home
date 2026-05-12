package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.enums.NoticeStatus;
import com.guitu.dto.NoticeDtos;
import com.guitu.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/api/notices")
    public ApiResponse<PageResponse<NoticeDtos.NoticeResponse>> publicList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(noticeService.listPublic(keyword, page, size));
    }

    @GetMapping("/api/notices/{id}")
    public ApiResponse<NoticeDtos.NoticeResponse> publicDetail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.detailPublic(id));
    }

    @GetMapping("/api/admin/notices")
    public ApiResponse<PageResponse<NoticeDtos.NoticeResponse>> adminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) NoticeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(noticeService.listAdmin(keyword, status, page, size));
    }

    @GetMapping("/api/admin/notices/{id}")
    public ApiResponse<NoticeDtos.NoticeResponse> adminDetail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.detailAdmin(id));
    }

    @PostMapping("/api/admin/notices")
    public ApiResponse<NoticeDtos.NoticeResponse> create(@Valid @RequestBody NoticeDtos.SaveNoticeRequest request) {
        return ApiResponse.ok(noticeService.create(request));
    }

    @PutMapping("/api/admin/notices/{id}")
    public ApiResponse<NoticeDtos.NoticeResponse> update(@PathVariable Long id, @Valid @RequestBody NoticeDtos.SaveNoticeRequest request) {
        return ApiResponse.ok(noticeService.update(id, request));
    }

    @PatchMapping("/api/admin/notices/{id}/offline")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/admin/notices/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.ok();
    }
}
