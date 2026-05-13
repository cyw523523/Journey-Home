package com.guitu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class SmartAdoptionDtos {
    private SmartAdoptionDtos() {
    }

    public record SmartMatchRequest(
            @NotNull(message = "动物编号不能为空")
            Long animalId,

            @NotBlank(message = "申请人姓名不能为空")
            @Size(max = 64, message = "申请人姓名长度不能超过64")
            String applicantName,

            @NotBlank(message = "联系方式不能为空")
            @Size(max = 64, message = "联系方式长度不能超过64")
            String contact,

            @NotBlank(message = "领养理由不能为空")
            @Size(max = 1000, message = "领养理由长度不能超过1000")
            String reason,

            @NotBlank(message = "居住条件不能为空")
            @Size(max = 1000, message = "居住条件长度不能超过1000")
            String livingCondition,

            @NotBlank(message = "饲养经验不能为空")
            @Size(max = 1000, message = "饲养经验长度不能超过1000")
            String experience,

            @NotBlank(message = "住房类型不能为空")
            @Size(max = 32, message = "住房类型长度不能超过32")
            String homeType,

            @NotBlank(message = "家庭结构不能为空")
            @Size(max = 32, message = "家庭结构长度不能超过32")
            String familyType,

            @NotBlank(message = "可投入时间不能为空")
            @Size(max = 32, message = "可投入时间长度不能超过32")
            String timeBudget,

            @NotBlank(message = "养宠经验等级不能为空")
            @Size(max = 32, message = "养宠经验等级长度不能超过32")
            String experienceLevel,

            @NotBlank(message = "期望陪伴类型不能为空")
            @Size(max = 32, message = "期望陪伴类型长度不能超过32")
            String companionExpectation,

            @Size(max = 1000, message = "补充问题长度不能超过1000")
            String question
    ) {
    }

    public record SmartMatchResponse(
            Long animalId,
            String animalTypeText,
            String model,
            String answer,
            LocalDateTime generatedAt
    ) {
    }
}
