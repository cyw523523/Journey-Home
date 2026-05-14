package com.guitu.dto;

import com.guitu.domain.enums.AnimalGender;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AnimalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AnimalDtos {
    private AnimalDtos() {
    }

    public record SaveAnimalRequest(
            @NotNull(message = "动物类型不能为空")
            AnimalType type,

            @NotNull(message = "动物性别不能为空")
            AnimalGender gender,

            Integer age,

            @NotBlank(message = "发现地区不能为空")
            @Size(max = 255, message = "发现地区长度不能超过255")
            String foundRegion,

            @Size(max = 500, message = "健康情况长度不能超过500")
            String healthCondition,

            @NotEmpty(message = "至少上传一张动物照片")
            List<@Size(max = 500, message = "图片路径长度不能超过500") String> imageUrls,

            @Size(max = 1000, message = "详细说明长度不能超过1000")
            String description,

            AnimalStatus status
    ) {
    }

    public record UpdateAnimalStatusRequest(
            @NotNull(message = "动物状态不能为空")
            AnimalStatus status
    ) {
    }

    public record AnimalResponse(
            Long id,
            AnimalType type,
            String typeText,
            AnimalGender gender,
            String genderText,
            Integer age,
            String foundRegion,
            String healthCondition,
            String coverImageUrl,
            List<String> imageUrls,
            String description,
            AnimalStatus status,
            String statusText,
            String reviewComment,
            Long publisherId,
            String publisherNickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
