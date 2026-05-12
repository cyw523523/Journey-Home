package com.guitu.dto;

import com.guitu.domain.enums.NoticeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class NoticeDtos {
    private NoticeDtos() {
    }

    public record SaveNoticeRequest(
            @NotBlank(message = "公告标题不能为空")
            @Size(max = 120, message = "公告标题长度不能超过120")
            String title,

            @NotBlank(message = "公告内容不能为空")
            String content,

            @NotNull(message = "公告状态不能为空")
            NoticeStatus status
    ) {
    }

    public record NoticeResponse(
            Long id,
            String title,
            String content,
            Long publisherId,
            String publisherNickname,
            LocalDateTime publishedAt,
            NoticeStatus status,
            String statusText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
