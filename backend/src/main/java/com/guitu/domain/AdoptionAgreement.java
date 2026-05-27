package com.guitu.domain;

import com.guitu.domain.enums.AdoptionAgreementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "adoption_agreements",
        uniqueConstraints = @UniqueConstraint(name = "uk_adoption_agreements_apply", columnNames = "apply_id")
)
public class AdoptionAgreement extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apply_id")
    private AdoptApply apply;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adopter_id")
    private User adopter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id")
    private User publisher;

    @Column(nullable = false, length = 64, unique = true)
    private String agreementNo;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdoptionAgreementStatus status = AdoptionAgreementStatus.PENDING_ADOPTER;

    @Column(length = 64)
    private String adopterSignatureName;

    @Column(length = 500)
    private String adopterSignatureImageUrl;

    private LocalDateTime adopterSignedAt;

    @Column(length = 64)
    private String counterpartSignatureName;

    @Column(length = 500)
    private String counterpartSignatureImageUrl;

    private LocalDateTime counterpartSignedAt;

    @Column(length = 500)
    private String pdfUrl;

    private LocalDateTime completedAt;
}
