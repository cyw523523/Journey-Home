package com.guitu.domain.enums;

public enum MedicalRecordType {
    DEWORMING("驱虫"),
    VACCINE("疫苗"),
    NEUTERING("绝育"),
    TREATMENT("诊疗"),
    OTHER("其他");

    private final String label;

    MedicalRecordType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
