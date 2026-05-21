package com.guitu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReplyResponse(
    Long id, String content, List<String> imageUrls,
    Long authorId, String authorNickname, String authorAvatarUrl, String authorRoleText,
    Long replyToUserId, String replyToUserNickname,
    LocalDateTime createdAt, String status, int likeCount, boolean liked,
    List mentions
) {}
