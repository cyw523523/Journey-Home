package com.guitu.domain;

import com.guitu.domain.enums.RescueStatus;
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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rescues")
public class Rescue extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 500)
    private String animalCondition;

    @Column(nullable = false, length = 64)
    private String contact;

    @Column(nullable = false, length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "rescue_images", joinColumns = @JoinColumn(name = "rescue_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RescueStatus status = RescueStatus.PENDING_REVIEW;

    @Column(length = 500)
    private String reviewComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id")
    private User publisher;
}
