package com.guitu.domain.enums;

public enum CommunityPostStatus {
    PUBLISHED("已发布"),
    OFFLINE("已下架");

    private final String label;

    CommunityPostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
