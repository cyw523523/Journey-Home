package com.guitu.domain.enums;

public enum AnimalStatus {
    PENDING_REVIEW("待审核"),
    WAITING_RESCUE("待救助"),
    RESCUING("救助中"),
    WAITING_ADOPTION("待领养"),
    ADOPTED("已领养"),
    OFFLINE("已下架"),
    REJECTED("已驳回");

    private final String label;

    AnimalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPublicVisible() {
        return this == WAITING_RESCUE || this == RESCUING || this == WAITING_ADOPTION || this == ADOPTED;
    }
}
