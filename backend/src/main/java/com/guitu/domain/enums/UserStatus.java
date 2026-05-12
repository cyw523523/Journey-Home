package com.guitu.domain.enums;

public enum UserStatus {
    NORMAL("正常"),
    DISABLED("禁用");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
