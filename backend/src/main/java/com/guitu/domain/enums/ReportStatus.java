package com.guitu.domain.enums;

public enum ReportStatus {
    PENDING_REVIEW("Pending review"),
    PROCESSING("Processing"),
    RESOLVED_VALID("Valid"),
    RESOLVED_INVALID("Invalid"),
    DISMISSED_DUPLICATE("Duplicate");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
