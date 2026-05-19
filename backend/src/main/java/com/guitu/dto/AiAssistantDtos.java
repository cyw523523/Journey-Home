package com.guitu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiAssistantDtos {

    public record ChatRequest(
            @NotBlank(message = "消息不能为空")
            @Size(max = 2000, message = "消息长度不能超过2000字符")
            String message
    ) {}

    public record ChatResponse(
            String reply,
            String model,
            java.time.LocalDateTime timestamp
    ) {}
}
