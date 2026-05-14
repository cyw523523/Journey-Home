package com.guitu.domain.enums;

public enum CommunityPostStatus {
    PENDING_REVIEW("Pending review"),
    PUBLISHED("Published"),
    REJECTED("Rejected"),
    OFFLINE("Offline");

    private final String label;

    CommunityPostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
