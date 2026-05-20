package com.guitu.domain.enums;

public enum CertificationStatus {
    PENDING("待审核"),
    APPROVED("已认证"),
    REJECTED("未通过");

    private final String label;

    CertificationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
