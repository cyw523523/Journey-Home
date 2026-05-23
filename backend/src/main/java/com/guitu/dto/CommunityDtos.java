package com.guitu.dto;

import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class CommunityDtos {
    private CommunityDtos() {
    }

    public record SavePostRequest(
            @NotBlank(message = "帖子标题不能为空")
            @Size(max = 120, message = "帖子标题长度不能超过120字符")
            String title,

            @NotBlank(message = "帖子内容不能为空")
            @Size(max = 5000, message = "帖子内容长度不能超过5000字符")
            String content,

            @NotNull(message = "分类不能为空")
            Long categoryId,

            List<@Size(max = 500, message = "图片URL长度不能超过500字符") String> imageUrls
    ) {
    }

    public record SaveCommentRequest(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 2000, message = "评论内容长度不能超过2000字符")
            String content,

            List<@Size(max = 500, message = "图片URL长度不能超过500字符") String> imageUrls,

            Long parentCommentId
    ) {
    }

    public record CommunityPostResponse(
            Long id,
            String title,
            String content,
            Long authorId,
            String authorNickname,
            String authorAvatarUrl,
            UserRole authorRole,
            String authorRoleText,
            CommunityPostStatus status,
            String statusText,
            List<String> imageUrls,
            long commentCount,
            int viewCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long categoryId,
            String categoryCode,
            String categoryName
    ) {
    }

    public record CommunityCommentResponse(
            Long id,
            Long postId,
            Long parentCommentId,
            Long replyToAuthorId,
            String replyToAuthorNickname,
            Long authorId,
            String authorNickname,
            String authorAvatarUrl,
            UserRole authorRole,
            String authorRoleText,
            String content,
            List<String> imageUrls,
            CommunityCommentStatus status,
            String statusText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CommunityPostDetailResponse(
            CommunityPostResponse post,
            List<CommunityCommentResponse> comments
    ) {
    }

    public record CategoryResponse(
            Long id, String code, String name, String nameEn,
            String description, String icon, int sortOrder,
            boolean enabled, long postCount
    ) {}

    public record SaveCategoryRequest(
            @NotBlank String code,
            @NotBlank String name,
            String nameEn,
            String description,
            String icon,
            int sortOrder,
            boolean enabled
    ) {}

    // --- Floor / Reply request DTOs (responses are top-level records) ---

    public record MentionInfo(Long userId, String nickname) {}

    public record SaveFloorRequest(@NotBlank String content, List<String> imageUrls) {}

    public record SaveReplyRequest(@NotNull Long replyToCommentId, @NotBlank String content, List<String> imageUrls) {}
}
