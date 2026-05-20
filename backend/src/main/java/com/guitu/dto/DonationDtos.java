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
            @NotBlank(message = "Title is required")
            @Size(max = 120, message = "Title must be at most 120 characters")
            String title,

            @NotNull(message = "Category is required")
            SupplyCategory category,

            @NotNull(message = "Target quantity is required")
            @Min(value = 1, message = "Target quantity must be at least 1")
            @Max(value = 10000, message = "Target quantity must be at most 10000")
            Integer targetQuantity,

            @NotBlank(message = "Description is required")
            @Size(max = 1000, message = "Description must be at most 1000 characters")
            String description,

            @Size(max = 255, message = "Contact name must be at most 255 characters")
            String contactName,

            @Size(max = 64, message = "Contact phone must be at most 64 characters")
            String contactPhone,

            @Size(max = 500, message = "Shipping address must be at most 500 characters")
            String shippingAddress,

            @Size(max = 500, message = "Image URL must be at most 500 characters")
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
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity,

            @Size(max = 32, message = "Delivery method must be at most 32 characters")
            String deliveryMethod,

            @Size(max = 500, message = "Tracking number must be at most 500 characters")
            String trackingNumber,

            @Size(max = 1000, message = "Message must be at most 1000 characters")
            String message,

            @Size(max = 64, message = "Display name must be at most 64 characters")
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
