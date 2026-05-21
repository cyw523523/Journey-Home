package com.guitu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FloorResponse(
    Long id, Integer floorNo, String content, List<String> imageUrls,
    Long authorId, String authorNickname, String authorAvatarUrl, String authorRoleText,
    LocalDateTime createdAt, String status, int likeCount, boolean liked,
    boolean isPostAuthor, int replyCount,
    List topReplies
) {}
