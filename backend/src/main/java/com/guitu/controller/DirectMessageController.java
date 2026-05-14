package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.dto.DirectMessageDtos;
import com.guitu.service.DirectMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class DirectMessageController {
    private final DirectMessageService directMessageService;

    public DirectMessageController(DirectMessageService directMessageService) {
        this.directMessageService = directMessageService;
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResponse<DirectMessageDtos.ConversationResponse>> conversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(directMessageService.listMine(page, size));
    }

    @GetMapping("/conversations/with/{userId}")
    public ApiResponse<DirectMessageDtos.ConversationDetailResponse> detailWithUser(@PathVariable Long userId) {
        return ApiResponse.ok(directMessageService.detailWithUser(userId));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<DirectMessageDtos.ConversationDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(directMessageService.detail(id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<DirectMessageDtos.MessageResponse> send(
            @PathVariable Long id,
            @Valid @RequestBody DirectMessageDtos.SendMessageRequest request
    ) {
        return ApiResponse.ok(directMessageService.send(id, request));
    }

    @GetMapping("/summary")
    public ApiResponse<DirectMessageDtos.UnreadSummary> summary() {
        return ApiResponse.ok(directMessageService.summary());
    }
}
