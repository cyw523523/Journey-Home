package com.guitu.domain.enums;

public enum NotificationType {
    SYSTEM("System"),
    AUDIT_RESULT("Audit result"),
    REPORT_UPDATE("Report update"),
    APPEAL_UPDATE("Appeal update"),
    REPORT_CREATED("New report"),
    APPEAL_CREATED("New appeal"),
    ACCOUNT_ACTION("Account action"),
    COMMENT_REPLY("Comment reply"),
    COMMUNITY_POST_COMMENTED("Community post commented"),
    COMMUNITY_COMMENT_REPLIED("Community comment replied"),
    COMMUNITY_POST_LIKED("Community post liked"),
    COMMUNITY_COMMENT_LIKED("Community comment liked"),
    COMMUNITY_MENTIONED("Community mentioned"),
    COMMUNITY_FOLLOWED_NEW_POST("Following new post");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
