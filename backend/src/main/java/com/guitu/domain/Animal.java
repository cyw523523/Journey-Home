package com.guitu.domain;

import com.guitu.domain.enums.AnimalGender;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AnimalType;
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
@Table(name = "animals")
public class Animal extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnimalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnimalGender gender;

    private Integer age;

    @Column(nullable = false, length = 255)
    private String foundRegion;

    @Column(length = 500)
    private String healthCondition;

    @Column(length = 500)
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "animal_images", joinColumns = @JoinColumn(name = "animal_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnimalStatus status = AnimalStatus.PENDING_REVIEW;

    @Column(length = 500)
    private String reviewComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id")
    private User publisher;
}
