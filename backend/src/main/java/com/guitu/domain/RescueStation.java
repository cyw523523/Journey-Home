package com.guitu.domain;

import com.guitu.domain.enums.CertificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rescue_stations")
public class RescueStation extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 120)
    private String stationName;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String address;

    @Column(length = 64)
    private String contactPhone;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CertificationStatus certificationStatus = CertificationStatus.PENDING;

    @Column(nullable = false)
    private Integer followerCount = 0;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;
}
