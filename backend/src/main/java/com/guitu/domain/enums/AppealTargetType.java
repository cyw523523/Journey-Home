package com.guitu.domain.enums;

public enum AppealTargetType {
    ANIMAL("Animal profile"),
    RESCUE("Rescue post"),
    ADOPT_APPLY("Adoption application"),
    COMMUNITY_POST("Community post"),
    COMMUNITY_COMMENT("Community comment"),
    USER_BAN("User ban");

    private final String label;

    AppealTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
