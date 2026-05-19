package com.guitu.controller;

import com.guitu.dto.AiAssistantDtos;
import com.guitu.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public AiAssistantDtos.ChatResponse chat(@Valid @RequestBody AiAssistantDtos.ChatRequest request) {
        return aiAssistantService.chat(request);
    }
}
