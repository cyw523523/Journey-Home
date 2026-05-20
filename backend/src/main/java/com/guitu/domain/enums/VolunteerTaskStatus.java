package com.guitu.domain.enums;

public enum VolunteerTaskStatus {
    PENDING_REVIEW("待审核"),
    RECRUITING("招募中"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已驳回");

    private final String label;

    VolunteerTaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPublicVisible() {
        return this == RECRUITING || this == IN_PROGRESS || this == COMPLETED;
    }
}
