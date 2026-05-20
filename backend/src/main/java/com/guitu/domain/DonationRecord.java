package com.guitu.domain;

import com.guitu.domain.enums.DonationStatus;
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
@Table(name = "donation_records")
public class DonationRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demand_id")
    private SupplyDemand demand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 32)
    private String deliveryMethod;

    @Column(length = 500)
    private String trackingNumber;

    @Column(length = 1000)
    private String message;

    @Column(length = 64)
    private String donorDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DonationStatus status = DonationStatus.CLAIMED;

    @Column
    private LocalDateTime completedAt;
}
