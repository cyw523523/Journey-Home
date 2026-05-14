package com.guitu.domain;

import com.guitu.domain.enums.AppealStatus;
import com.guitu.domain.enums.AppealTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
@Table(name = "appeal_records")
public class AppealRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppealTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppealStatus status = AppealStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_reviewer_id")
    private User firstReviewer;

    @Column(length = 500)
    private String firstReviewOpinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_reviewer_id")
    private User secondReviewer;

    @Column(length = 500)
    private String finalReviewOpinion;

    private LocalDateTime reviewedAt;
}
