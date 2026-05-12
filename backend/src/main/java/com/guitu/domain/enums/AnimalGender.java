package com.guitu.domain.enums;

public enum AnimalGender {
    MALE("公"),
    FEMALE("母"),
    UNKNOWN("未知");

    private final String label;

    AnimalGender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
