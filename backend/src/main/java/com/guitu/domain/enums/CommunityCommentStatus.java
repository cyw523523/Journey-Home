package com.guitu.domain.enums;

public enum CommunityCommentStatus {
    PUBLISHED("Published"),
    PENDING_REVIEW("Pending review"),
    REJECTED("Rejected"),
    OFFLINE("Offline");

    private final String label;

    CommunityCommentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
