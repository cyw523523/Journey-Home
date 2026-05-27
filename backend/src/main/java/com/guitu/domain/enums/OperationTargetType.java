package com.guitu.domain.enums;

public enum OperationTargetType {
    ANIMAL("动物档案"),
    RESCUE("救助信息"),
    ADOPT_APPLY("领养申请"),
    ADOPTION_AGREEMENT("领养协议"),
    ADOPTION_FOLLOW_UP("领养回访"),
    COMMUNITY_POST("社区帖子"),
    COMMUNITY_COMMENT("社区评论"),
    NOTICE("公告"),
    USER("用户");

    private final String label;

    OperationTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
