package com.guitu.domain.enums;

public enum ReportReasonType {
    FALSE_INFORMATION("False information"),
    ADVERTISEMENT("Advertisement"),
    ABUSE("Abusive content"),
    SENSITIVE_IMAGE("Sensitive image"),
    HARASSMENT("Harassment"),
    SPAM("Spam"),
    OTHER("Other");

    private final String label;

    ReportReasonType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
