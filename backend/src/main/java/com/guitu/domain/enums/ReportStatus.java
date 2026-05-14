package com.guitu.domain.enums;

public enum ReportStatus {
    PENDING_REVIEW("待处理"),
    PROCESSING("处理中"),
    RESOLVED_VALID("举报属实"),
    RESOLVED_INVALID("举报不属实"),
    DISMISSED_DUPLICATE("重复举报");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
