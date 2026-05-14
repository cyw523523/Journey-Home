package com.guitu.domain.enums;

public enum ReportReasonType {
    FALSE_INFORMATION("虚假信息"),
    ADVERTISEMENT("广告引流"),
    ABUSE("不良内容"),
    SENSITIVE_IMAGE("敏感图片"),
    HARASSMENT("辱骂骚扰"),
    SPAM("垃圾刷屏"),
    OTHER("其他");

    private final String label;

    ReportReasonType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
