package com.guitu.domain.enums;

public enum AuditAction {
    APPROVE("通过"),
    REJECT("驳回"),
    OFFLINE("下架");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
