package com.guitu.domain.enums;

public enum NoticeStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    OFFLINE("已下架");

    private final String label;

    NoticeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
