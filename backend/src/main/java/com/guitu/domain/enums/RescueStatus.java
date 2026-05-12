package com.guitu.domain.enums;

public enum RescueStatus {
    PENDING_REVIEW("待审核"),
    PENDING_PROCESS("待处理"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    OFFLINE("已下架"),
    REJECTED("已驳回");

    private final String label;

    RescueStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPublicVisible() {
        return this == PENDING_PROCESS || this == PROCESSING || this == COMPLETED;
    }
}
