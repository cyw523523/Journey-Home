package com.guitu.domain.enums;

public enum AdoptionFollowUpStatus {
    PENDING("待回访"),
    COMPLETED("已回访");

    private final String label;

    AdoptionFollowUpStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
