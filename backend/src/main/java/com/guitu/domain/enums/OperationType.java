package com.guitu.domain.enums;

public enum OperationType {
    CREATE("创建"),
    UPDATE("编辑"),
    OFFLINE("下架"),
    STATUS_CHANGE("状态变更"),
    SUBMIT_APPLICATION("提交申请"),
    CANCEL_APPLICATION("取消申请"),
    APPROVE_APPLICATION("通过申请"),
    REJECT_APPLICATION("驳回申请"),
    CREATE_AGREEMENT("生成协议"),
    SIGN_AGREEMENT("签署协议"),
    CREATE_FOLLOW_UP_PLAN("生成回访计划"),
    COMPLETE_FOLLOW_UP("完成回访");

    private final String label;

    OperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
