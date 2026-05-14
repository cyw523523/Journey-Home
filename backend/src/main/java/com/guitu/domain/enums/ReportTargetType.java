package com.guitu.domain.enums;

public enum ReportTargetType {
    ANIMAL("Animal profile"),
    RESCUE("Rescue post"),
    COMMUNITY_POST("Community post"),
    COMMUNITY_COMMENT("Community comment"),
    USER("User");

    private final String label;

    ReportTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
