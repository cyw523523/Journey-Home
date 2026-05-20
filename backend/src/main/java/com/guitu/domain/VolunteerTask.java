package com.guitu.domain;

import com.guitu.domain.enums.VolunteerTaskStatus;
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
@Table(name = "volunteer_tasks")
public class VolunteerTask extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false)
    private Integer maxVolunteers = 1;

    @Column(nullable = false)
    private Integer currentVolunteers = 0;

    @Column
    private LocalDateTime scheduledTime;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VolunteerTaskStatus status = VolunteerTaskStatus.PENDING_REVIEW;

    @Column(length = 500)
    private String reviewComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id")
    private User publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_rescue_id")
    private Rescue relatedRescue;
}
