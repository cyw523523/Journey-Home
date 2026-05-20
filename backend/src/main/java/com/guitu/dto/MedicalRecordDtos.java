package com.guitu.dto;

import com.guitu.domain.enums.MedicalRecordType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MedicalRecordDtos {
    private MedicalRecordDtos() {
    }

    public record SaveMedicalRecordRequest(
            @NotNull(message = "医疗记录类型不能为空")
            MedicalRecordType type,

            @NotNull(message = "日期不能为空")
            LocalDate recordDate,

            @Size(max = 64, message = "兽医姓名长度不能超过64")
            String veterinarianName,

            @Size(max = 255, message = "机构名称长度不能超过255")
            String institution,

            @Size(max = 500, message = "描述长度不能超过500")
            String description,

            List<@Size(max = 500, message = "图片路径长度不能超过500") String> imageUrls
    ) {
    }

    public record MedicalRecordResponse(
            Long id,
            MedicalRecordType type,
            String typeText,
            LocalDate recordDate,
            String veterinarianName,
            String institution,
            String description,
            List<String> imageUrls,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
