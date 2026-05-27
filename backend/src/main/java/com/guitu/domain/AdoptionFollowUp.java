package com.guitu.domain;

import com.guitu.domain.enums.AdoptionFollowUpStatus;
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
@Table(name = "adoption_follow_ups")
public class AdoptionFollowUp extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apply_id")
    private AdoptApply apply;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adopter_id")
    private User adopter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false, length = 32)
    private String stageCode;

    @Column(nullable = false, length = 64)
    private String stageLabel;

    @Column(nullable = false)
    private LocalDateTime plannedAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdoptionFollowUpStatus status = AdoptionFollowUpStatus.PENDING;

    @Column(length = 1000)
    private String note;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "adoption_follow_up_images", joinColumns = @JoinColumn(name = "follow_up_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();
}
