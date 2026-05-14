package com.guitu.domain;

import com.guitu.domain.enums.ReportReasonType;
import com.guitu.domain.enums.ReportResolutionAction;
import com.guitu.domain.enums.ReportStatus;
import com.guitu.domain.enums.ReportTargetType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "content_reports")
public class ContentReport extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_owner_id")
    private User targetOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportReasonType reasonType;

    @Column(nullable = false, length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_report_images", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "image_url", length = 500)
    private List<String> evidenceImageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportStatus status = ReportStatus.PENDING_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ReportResolutionAction resolutionAction;

    @Column(length = 500)
    private String resolutionOpinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    private LocalDateTime reviewedAt;
}
