package com.guitu.domain.enums;

public enum AnimalType {
    CAT("猫"),
    DOG("狗"),
    OTHER("其他");

    private final String label;

    AnimalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
