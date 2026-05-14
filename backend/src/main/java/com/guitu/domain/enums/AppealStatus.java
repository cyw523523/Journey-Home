package com.guitu.domain.enums;

public enum AppealStatus {
    PENDING_REVIEW("Pending review"),
    SECOND_REVIEW_PENDING("Second review pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    AppealStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
