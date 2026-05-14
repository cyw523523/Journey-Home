package com.guitu.domain.enums;

public enum AuditTargetType {
    ANIMAL("Animal profile"),
    RESCUE("Rescue post"),
    ADOPT_APPLY("Adoption application"),
    COMMUNITY_POST("Community post"),
    COMMUNITY_COMMENT("Community comment");

    private final String label;

    AuditTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
