package com.guitu.domain.enums;

public enum ReportResolutionAction {
    DISMISS("Dismiss"),
    WARN_ONLY("Warn only"),
    OFFLINE_CONTENT("Offline content"),
    BAN_USER("Ban user");

    private final String label;

    ReportResolutionAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
