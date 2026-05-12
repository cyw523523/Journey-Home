package com.guitu.domain.enums;

public enum AuditTargetType {
    ANIMAL("动物档案"),
    RESCUE("救助信息"),
    ADOPT_APPLY("领养申请");

    private final String label;

    AuditTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
