package com.guitu.dto;

import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class CommunityDtos {
    private CommunityDtos() {
    }

    public record SavePostRequest(
            @NotBlank(message = "帖子标题不能为空")
            @Size(max = 120, message = "帖子标题长度不能超过120")
            String title,

            @NotBlank(message = "帖子内容不能为空")
            @Size(max = 5000, message = "帖子内容长度不能超过5000")
            String content,

            List<@Size(max = 500, message = "图片路径长度不能超过500") String> imageUrls
    ) {
    }

    public record SaveCommentRequest(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 2000, message = "评论内容长度不能超过2000")
            String content,

            List<@Size(max = 500, message = "图片路径长度不能超过500") String> imageUrls,

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
            LocalDateTime createdAt,
            LocalDateTime updatedAt
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CommunityPostDetailResponse(
            CommunityPostResponse post,
            List<CommunityCommentResponse> comments
    ) {
    }
}
