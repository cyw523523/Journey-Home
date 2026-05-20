package com.guitu.domain.enums;

public enum ApplicationStatus {
    PENDING("待确认"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    WITHDRAWN("已撤回"),
    COMPLETED("已完成");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
