package com.guitu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitu.domain.Animal;
import com.guitu.domain.User;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.dto.SmartAdoptionDtos;
import com.guitu.exception.BusinessException;
import com.guitu.repository.AdoptApplyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SmartAdoptionService {
    private final AnimalService animalService;
    private final UserService userService;
    private final AdoptApplyRepository adoptApplyRepository;
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

    public SmartAdoptionService(
            AnimalService animalService,
            UserService userService,
            AdoptApplyRepository adoptApplyRepository,
            ObjectMapper objectMapper
    ) {
        this.animalService = animalService;
        this.userService = userService;
        this.adoptApplyRepository = adoptApplyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SmartAdoptionDtos.SmartMatchResponse generateAdvice(SmartAdoptionDtos.SmartMatchRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "未配置智谱 AI API Key，请先设置 ZHIPU_API_KEY");
        }

        Animal animal = animalService.getEntity(request.animalId());
        if (!animal.getStatus().isPublicVisible()) {
            throw new BusinessException("当前动物暂不支持生成智能领养建议");
        }

        User currentUser = userService.currentUser();
        String answer = callModel(animal, currentUser, request);
        return new SmartAdoptionDtos.SmartMatchResponse(
                animal.getId(),
                animal.getType().getLabel(),
                model,
                answer,
                LocalDateTime.now()
        );
    }

    private String callModel(Animal animal, User currentUser, SmartAdoptionDtos.SmartMatchRequest request) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
                .build();

        ModelRequest payload = new ModelRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt()),
                        new ChatMessage("user", buildUserPrompt(animal, currentUser, request))
                ),
                false,
                0.4,
                1500
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
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "智谱 AI 调用失败，状态码: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = readContent(contentNode);
            if (content.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "智谱 AI 未返回有效内容");
            }
            return content.trim();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "智谱 AI 返回内容无法解析");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "智谱 AI 请求被中断");
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
                你是流浪动物领养平台的 AI 领养顾问。
                你必须严格基于用户输入和系统提供的数据库内容，生成一份中文领养分析建议。
                不要虚构数据库中没有的信息；如果信息不足，要明确指出“信息不足，需要线下进一步确认”。
                请直接输出给用户看的自然中文，不要输出 JSON，不要输出 Markdown 代码块。
                建议内容要覆盖以下四部分，但语言要自然：
                1. 总体适配判断
                2. 为什么适合或不适合
                3. 需要重点确认的风险点
                4. 申请前后建议做的准备
                语气要求专业、温和、具体，避免空话套话。
                """;
    }

    private String buildUserPrompt(Animal animal, User currentUser, SmartAdoptionDtos.SmartMatchRequest request) {
        long totalApplies = adoptApplyRepository.countByAnimalId(animal.getId());
        long pendingApplies = adoptApplyRepository.countByAnimalIdAndStatus(animal.getId(), ApplyStatus.PENDING_REVIEW);
        long approvedApplies = adoptApplyRepository.countByAnimalIdAndStatus(animal.getId(), ApplyStatus.APPROVED);

        return """
                请根据以下真实业务数据，为这位用户生成一份智能领养建议。

                一、动物档案（来自数据库）
                - 动物ID：%s
                - 类型：%s
                - 性别：%s
                - 年龄：%s
                - 发现地区：%s
                - 健康情况：%s
                - 档案描述：%s
                - 当前状态：%s
                - 发布人昵称：%s

                二、该动物相关的领养申请数据（来自数据库）
                - 历史申请总数：%s
                - 当前待审核申请数：%s
                - 已通过申请数：%s

                三、当前登录用户资料（来自数据库）
                - 用户ID：%s
                - 昵称：%s
                - 手机号：%s
                - 角色：%s

                四、用户本次填写的领养资料
                - 申请人姓名：%s
                - 联系方式：%s
                - 领养理由：%s
                - 居住条件：%s
                - 饲养经验：%s
                - 住房类型：%s
                - 家庭结构：%s
                - 可投入时间：%s
                - 养宠经验等级：%s
                - 期望陪伴风格：%s
                - 额外想咨询 AI 的问题：%s

                请结合以上内容，给出一份面向申请人的直接建议。
                如果存在健康照护、居住限制、时间不足、经验不足、情绪适配、竞争激烈等风险，请明确指出。
                """.formatted(
                animal.getId(),
                animal.getType().getLabel(),
                animal.getGender().getLabel(),
                animal.getAge() == null ? "未知" : animal.getAge() + "岁",
                safe(animal.getFoundRegion()),
                safe(animal.getHealthCondition()),
                safe(animal.getDescription()),
                animal.getStatus().getLabel(),
                animal.getPublisher() == null ? "未知" : safe(animal.getPublisher().getNickname()),
                totalApplies,
                pendingApplies,
                approvedApplies,
                currentUser.getId(),
                safe(currentUser.getNickname()),
                safe(currentUser.getPhone()),
                currentUser.getRole().getLabel(),
                safe(request.applicantName()),
                safe(request.contact()),
                safe(request.reason()),
                safe(request.livingCondition()),
                safe(request.experience()),
                safe(request.homeType()),
                safe(request.familyType()),
                safe(request.timeBudget()),
                safe(request.experienceLevel()),
                safe(request.companionExpectation()),
                safe(request.question())
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未提供" : value.trim();
    }

    private record ModelRequest(
            String model,
            List<ChatMessage> messages,
            boolean stream,
            double temperature,
            int max_tokens
    ) {
    }

    private record ChatMessage(String role, String content) {
    }
}
