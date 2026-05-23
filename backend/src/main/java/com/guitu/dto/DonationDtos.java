package com.guitu.dto;

import com.guitu.domain.enums.DonationStatus;
import com.guitu.domain.enums.SupplyCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class DonationDtos {
    private DonationDtos() {
    }

    public record SaveSupplyDemandRequest(
            @NotBlank(message = "标题不能为空")
            @Size(max = 120, message = "标题长度不能超过120字符")
            String title,

            @NotNull(message = "物资分类不能为空")
            SupplyCategory category,

            @NotNull(message = "目标数量不能为空")
            @Min(value = 1, message = "目标数量至少为1")
            @Max(value = 10000, message = "目标数量最多为10000")
            Integer targetQuantity,

            @NotBlank(message = "描述不能为空")
            @Size(max = 1000, message = "描述长度不能超过1000字符")
            String description,

            @Size(max = 255, message = "联系人姓名长度不能超过255字符")
            String contactName,

            @Size(max = 64, message = "联系电话长度不能超过64字符")
            String contactPhone,

            @Size(max = 500, message = "收货地址长度不能超过500字符")
            String shippingAddress,

            @Size(max = 500, message = "图片URL长度不能超过500字符")
            String imageUrl
    ) {
    }

    public record SupplyDemandResponse(
            Long id,
            String title,
            SupplyCategory category,
            String categoryLabel,
            Integer targetQuantity,
            Integer currentQuantity,
            String description,
            String contactName,
            String contactPhone,
            String shippingAddress,
            DonationStatus status,
            String statusText,
            String imageUrl,
            Long publisherId,
            String publisherNickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record DonateRequest(
            @NotNull(message = "捐赠数量不能为空")
            @Min(value = 1, message = "捐赠数量至少为1")
            Integer quantity,

            @Size(max = 32, message = "配送方式长度不能超过32字符")
            String deliveryMethod,

            @Size(max = 500, message = "物流单号长度不能超过500字符")
            String trackingNumber,

            @Size(max = 1000, message = "留言长度不能超过1000字符")
            String message,

            @Size(max = 64, message = "显示名称长度不能超过64字符")
            String donorDisplayName
    ) {
    }

    public record DonationRecordResponse(
            Long id,
            Long demandId,
            String demandTitle,
            Long donorId,
            String donorDisplayName,
            Integer quantity,
            String deliveryMethod,
            String trackingNumber,
            String message,
            DonationStatus status,
            String statusText,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {
    }
}
