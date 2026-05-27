package com.guitu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class AiAssistantDtos {

    public record ChatRequest(
            @NotBlank(message = "消息不能为空")
            @Size(max = 2000, message = "消息长度不能超过 2000 个字符")
            String message,
            java.util.List<ConversationMessage> history,
            PageContext pageContext
    ) {}

    public record ConversationMessage(
            @Size(max = 20, message = "role 过长")
            String role,

            @Size(max = 4000, message = "历史消息过长")
            String content
    ) {}

    public record PageContext(
            @Size(max = 80, message = "routeName 过长")
            String routeName,

            @Size(max = 255, message = "routePath 过长")
            String routePath,

            @Size(max = 120, message = "pageTitle 过长")
            String pageTitle,

            @Size(max = 500, message = "pageSummary 过长")
            String pageSummary,

            @Size(max = 50, message = "entityType 过长")
            String entityType,

            Long entityId,

            Map<String, Object> viewData
    ) {}

    public record ChatResponse(
            String reply,
            String model,
            java.time.LocalDateTime timestamp
    ) {}
}
