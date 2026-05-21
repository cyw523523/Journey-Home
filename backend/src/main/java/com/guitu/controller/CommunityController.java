package com.guitu.controller;

import com.guitu.common.ApiResponse;
import com.guitu.common.PageResponse;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.User;
import com.guitu.dto.CommunityDtos;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.security.SecuritySupport;
import com.guitu.service.CommunityCommentService;
import com.guitu.service.CommunityFavoriteService;
import com.guitu.service.CommunityLikeService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService communityService;
    private final CommunityLikeService likeService;
    private final CommunityFavoriteService favoriteService;
    private final CommunityCommentService commentService;
    private final CommunityPostRepository postRepo;

    public CommunityController(CommunityService communityService, CommunityLikeService likeService,
            CommunityFavoriteService favoriteService, CommunityCommentService commentService,
            CommunityPostRepository postRepo) {
        this.communityService = communityService;
        this.likeService = likeService;
        this.favoriteService = favoriteService;
        this.commentService = commentService;
        this.postRepo = postRepo;
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
        commentService.deleteComment(id);
        return ApiResponse.ok();
    }

    // --- Floor / Reply endpoints ---

    @GetMapping("/posts/{id}/floors")
    public ApiResponse<PageResponse<CommunityDtos.FloorResponse>> listFloors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean onlyAuthor,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CommunityPost p = postRepo.findById(id).orElseThrow();
        Long currentUserId = SecuritySupport.currentUser()
                .map(sp -> sp.id()).orElse(null);
        return ApiResponse.ok(commentService.listFloors(id, currentUserId, onlyAuthor,
                "desc".equals(order), p.getAuthor().getId(), page, size));
    }

    @GetMapping("/comments/{floorId}/replies")
    public ApiResponse<PageResponse<CommunityDtos.ReplyResponse>> listReplies(
            @PathVariable Long floorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = SecuritySupport.currentUser()
                .map(sp -> sp.id()).orElse(null);
        return ApiResponse.ok(commentService.listReplies(floorId, currentUserId, page, size));
    }

    @PostMapping("/posts/{id}/floors")
    public ApiResponse<CommunityDtos.FloorResponse> createFloor(@PathVariable Long id,
            @Valid @RequestBody CommunityDtos.SaveFloorRequest req) {
        return ApiResponse.ok(commentService.createFloor(id, req));
    }

    @PostMapping("/comments/{floorId}/replies")
    public ApiResponse<CommunityDtos.ReplyResponse> createReply(@PathVariable Long floorId,
            @Valid @RequestBody CommunityDtos.SaveReplyRequest req) {
        return ApiResponse.ok(commentService.createReply(floorId, req));
    }

    // --- Like / Favorite ---

    @PostMapping("/likes")
    public ApiResponse<Boolean> like(@RequestBody Map<String, Object> body) {
        Long targetId = ((Number) body.get("targetId")).longValue();
        String targetType = (String) body.get("targetType");
        boolean liked = likeService.toggle(SecuritySupport.requireUser().id(), targetType, targetId);
        return ApiResponse.ok(liked);
    }

    @DeleteMapping("/likes")
    public ApiResponse<Boolean> unlike(@RequestBody Map<String, Object> body) {
        Long targetId = ((Number) body.get("targetId")).longValue();
        String targetType = (String) body.get("targetType");
        return ApiResponse.ok(likeService.toggle(SecuritySupport.requireUser().id(), targetType, targetId));
    }

    @PostMapping("/posts/{id}/favorite")
    public ApiResponse<Boolean> favorite(@PathVariable Long id) {
        return ApiResponse.ok(favoriteService.toggle(SecuritySupport.requireUser().id(), id));
    }

    @DeleteMapping("/posts/{id}/favorite")
    public ApiResponse<Boolean> unfavorite(@PathVariable Long id) {
        return ApiResponse.ok(favoriteService.toggle(SecuritySupport.requireUser().id(), id));
    }
}
