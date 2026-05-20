package com.guitu.domain.enums;

public enum SupplyCategory {
    CAT_FOOD("猫粮"),
    DOG_FOOD("狗粮"),
    CAT_LITTER("猫砂"),
    MEDICINE("药品"),
    TOYS("玩具"),
    BEDDING("垫子/窝"),
    CLEANING("清洁用品"),
    OTHER("其他");

    private final String label;

    SupplyCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
