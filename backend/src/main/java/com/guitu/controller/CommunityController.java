package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.dto.CommunityDtos;
import com.guitu.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<CommunityDtos.CommunityPostResponse>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(communityService.listPublic(keyword, page, size));
    }

    @GetMapping("/mine/posts")
    public ApiResponse<PageResponse<CommunityDtos.CommunityPostResponse>> listMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(communityService.listMine(page, size));
    }

    @GetMapping("/mine/comments")
    public ApiResponse<PageResponse<CommunityDtos.CommunityCommentResponse>> listMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(communityService.listMyComments(page, size));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<CommunityDtos.CommunityPostDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(communityService.detailPublic(id));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityDtos.CommunityPostResponse> create(@Valid @RequestBody CommunityDtos.SavePostRequest request) {
        return ApiResponse.ok(communityService.create(request));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<CommunityDtos.CommunityPostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CommunityDtos.SavePostRequest request
    ) {
        return ApiResponse.ok(communityService.update(id, request));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        communityService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<CommunityDtos.CommunityCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommunityDtos.SaveCommentRequest request
    ) {
        return ApiResponse.ok(communityService.addComment(id, request));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        communityService.deleteComment(id);
        return ApiResponse.ok();
    }
}
