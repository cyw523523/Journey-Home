package com.guitu.domain;

import com.guitu.domain.enums.ApplyStatus;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "adopt_applies")
public class AdoptApply extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @Column(nullable = false, length = 64)
    private String applicantName;

    @Column(nullable = false, length = 64)
    private String contact;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 1000)
    private String livingCondition;

    @Column(nullable = false, length = 1000)
    private String experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplyStatus status = ApplyStatus.PENDING_REVIEW;

    @Column(length = 500)
    private String auditOpinion;
}
