package com.guitu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitu.dto.AiAssistantDtos;
import com.guitu.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiAssistantService {
    private final ObjectMapper objectMapper;

    @Value("${app.ai.zhipu.api-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String apiUrl;

    @Value("${app.ai.zhipu.api-key:}")
    private String apiKey;

    @Value("${app.ai.zhipu.model:glm-4-flash-250414}")
    private String model;

    @Value("${app.ai.zhipu.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.ai.zhipu.read-timeout-ms:30000}")
    private int readTimeoutMs;

    public AiAssistantService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAssistantDtos.ChatResponse chat(AiAssistantDtos.ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI助手服务暂未配置，请联系管理员");
        }

        String reply = callModel(request.message());
        return new AiAssistantDtos.ChatResponse(reply, model, LocalDateTime.now());
    }

    private String callModel(String userMessage) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
                .build();

        ModelRequest payload = new ModelRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt()),
                        new ChatMessage("user", userMessage)
                ),
                false,
                0.7,
                1000
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 3000)))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI服务暂时不可用，请稍后再试");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = readContent(contentNode);
            if (content.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI助手暂时无法回答，请换个问题试试");
            }
            return content.trim();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI服务响应异常，请稍后再试");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI服务请求被中断");
        }
    }

    private String readContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private String systemPrompt() {
        return """
                你是"归途"流浪动物救助领养平台的AI智能助手。
                你的职责是帮助用户了解流浪动物救助、领养相关知识，解答用户关于平台使用的问题。
                
                回答要求：
                1. 语气友好、温暖、专业
                2. 使用清晰的分段和换行
                3. 对于步骤类回答，使用数字序号（如 1. 2. 3.）
                4. 对于列表类内容，使用短横线 "- " 开头
                5. 不要使用Markdown格式（如**加粗**、`代码`等）
                6. 回答简洁明了，避免冗长
                7. 对于紧急医疗问题，建议用户及时就医
                8. 仅使用中文回答
                
                回答示例格式：
                您好！发布救助信息请按以下步骤操作：
                
                1. 注册登录：在平台注册账号并登录
                
                2. 进入发布页面：点击首页的"发布救助信息"按钮
                
                3. 填写信息：
                - 动物基本信息（品种、年龄、性别）
                - 救助地点和时间
                - 健康状况和特殊需求
                - 联系方式
                - 上传清晰照片
                
                4. 提交审核：检查信息无误后点击提交
                
                如果有任何疑问，欢迎随时联系我们！
                """;
    }

    private record ModelRequest(
            String model,
            List<ChatMessage> messages,
            boolean stream,
            double temperature,
            int max_tokens
    ) {}

    private record ChatMessage(String role, String content) {}
}
