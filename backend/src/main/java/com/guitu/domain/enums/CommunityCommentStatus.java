package com.guitu.domain.enums;

public enum CommunityCommentStatus {
    PENDING_REVIEW("Pending review"),
    PUBLISHED("Published"),
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
