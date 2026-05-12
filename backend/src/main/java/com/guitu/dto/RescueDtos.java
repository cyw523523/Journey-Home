package com.guitu.dto;

import com.guitu.domain.enums.RescueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class RescueDtos {
    private RescueDtos() {
    }

    public record SaveRescueRequest(
            @NotBlank(message = "救助地点不能为空")
            @Size(max = 255, message = "救助地点长度不能超过255")
            String location,

            @NotBlank(message = "动物情况不能为空")
            @Size(max = 500, message = "动物情况长度不能超过500")
            String animalCondition,

            @NotBlank(message = "联系方式不能为空")
            @Size(max = 64, message = "联系方式长度不能超过64")
            String contact,

            @NotBlank(message = "求助说明不能为空")
            @Size(max = 1000, message = "求助说明长度不能超过1000")
            String description,

            List<@Size(max = 500, message = "图片路径长度不能超过500") String> imageUrls
    ) {
    }

    public record UpdateRescueStatusRequest(
            @NotNull(message = "救助状态不能为空")
            RescueStatus status
    ) {
    }

    public record RescueResponse(
            Long id,
            String location,
            String animalCondition,
            String contact,
            String description,
            List<String> imageUrls,
            RescueStatus status,
            String statusText,
            String reviewComment,
            Long publisherId,
            String publisherNickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
