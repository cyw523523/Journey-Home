package com.guitu.domain;

import com.guitu.domain.enums.OperationTargetType;
import com.guitu.domain.enums.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "operation_logs")
public class OperationLog extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private OperationTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 255)
    private String targetName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private OperationType operationType;

    @Column(length = 64)
    private String operatorIp;

    @Column(length = 500)
    private String detail;

    @Lob
    private String beforeSnapshot;

    @Lob
    private String afterSnapshot;

    @Column(nullable = false)
    private LocalDateTime operatedAt;
}
