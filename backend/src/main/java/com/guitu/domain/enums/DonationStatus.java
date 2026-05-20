package com.guitu.domain.enums;

public enum DonationStatus {
    PENDING("待认领"),
    CLAIMED("已认领"),
    IN_TRANSIT("运输中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    DonationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPublicVisible() {
        return this == PENDING || this == CLAIMED || this == IN_TRANSIT || this == COMPLETED;
    }
}
