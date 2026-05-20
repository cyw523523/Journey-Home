package com.guitu.domain;

import com.guitu.domain.enums.MedicalRecordType;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medical_records")
public class MedicalRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MedicalRecordType type;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(length = 64)
    private String veterinarianName;

    @Column(length = 255)
    private String institution;

    @Column(length = 500)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medical_record_images", joinColumns = @JoinColumn(name = "medical_record_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();
}
