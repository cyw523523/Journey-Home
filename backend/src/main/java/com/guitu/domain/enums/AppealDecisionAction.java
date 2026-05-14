package com.guitu.domain.enums;

public enum AppealDecisionAction {
    ESCALATE("Escalate"),
    APPROVE("Approve"),
    REJECT("Reject");

    private final String label;

    AppealDecisionAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
