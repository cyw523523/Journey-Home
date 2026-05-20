package com.guitu.domain;

import com.guitu.domain.enums.DonationStatus;
import com.guitu.domain.enums.SupplyCategory;
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
@Table(name = "supply_demands")
public class SupplyDemand extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupplyCategory category;

    @Column(nullable = false)
    private Integer targetQuantity;

    @Column(nullable = false)
    private Integer currentQuantity = 0;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 255)
    private String contactName;

    @Column(length = 64)
    private String contactPhone;

    @Column(length = 500)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DonationStatus status = DonationStatus.PENDING;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id")
    private User publisher;
}
