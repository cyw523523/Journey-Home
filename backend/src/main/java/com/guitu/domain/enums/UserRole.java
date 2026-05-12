package com.guitu.domain.enums;

public enum UserRole {
    USER("普通用户"),
    RESCUER("救助信息发布者"),
    ADMIN("管理员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
