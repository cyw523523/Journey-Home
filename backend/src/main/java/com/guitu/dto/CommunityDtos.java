package com.guitu.dto;

import com.guitu.domain.enums.CommunityCommentStatus;
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
            @NotBlank(message = "Post title is required")
            @Size(max = 120, message = "Post title must be at most 120 characters")
            String title,

            @NotBlank(message = "Post content is required")
            @Size(max = 5000, message = "Post content must be at most 5000 characters")
            String content,

            List<@Size(max = 500, message = "Image URL must be at most 500 characters") String> imageUrls
    ) {
    }

    public record SaveCommentRequest(
            @NotBlank(message = "Comment content is required")
            @Size(max = 2000, message = "Comment content must be at most 2000 characters")
            String content,

            List<@Size(max = 500, message = "Image URL must be at most 500 characters") String> imageUrls,

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
}
