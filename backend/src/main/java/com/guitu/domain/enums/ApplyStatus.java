package com.guitu.domain.enums;

public enum ApplyStatus {
    PENDING_REVIEW("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    CANCELED("已取消");

    private final String label;

    ApplyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
