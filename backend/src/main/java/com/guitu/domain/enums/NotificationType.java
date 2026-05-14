package com.guitu.domain.enums;

public enum NotificationType {
    SYSTEM("System"),
    AUDIT_RESULT("Audit result"),
    REPORT_UPDATE("Report update"),
    APPEAL_UPDATE("Appeal update"),
    REPORT_CREATED("New report"),
    APPEAL_CREATED("New appeal"),
    ACCOUNT_ACTION("Account action"),
    COMMENT_REPLY("Comment reply");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
