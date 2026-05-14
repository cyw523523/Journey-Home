package com.guitu.domain.enums;

public enum ReportTargetType {
    ANIMAL("动物档案"),
    RESCUE("救助信息"),
    COMMUNITY_POST("社区帖子"),
    COMMUNITY_COMMENT("社区评论"),
    USER("用户");

    private final String label;

    ReportTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
