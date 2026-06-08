package com.guitu.domain.enums;

public enum AdoptionAgreementStatus {
    PENDING_ADOPTER("待领养人签署"),
    PENDING_COUNTERPART("待救助方签署"),
    COMPLETED("已签署完成");

    private final String label;

    AdoptionAgreementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
