package com.guitu.dto;

import com.guitu.domain.enums.ApplyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AdoptApplyDtos {
    private AdoptApplyDtos() {
    }

    public record CreateApplyRequest(
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
            String experience
    ) {
    }

    public record ApplyResponse(
            Long id,
            Long animalId,
            String animalTypeText,
            Long applicantId,
            String applicantName,
            String contact,
            String reason,
            String livingCondition,
            String experience,
            ApplyStatus status,
            String statusText,
            String auditOpinion,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
