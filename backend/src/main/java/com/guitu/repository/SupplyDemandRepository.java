package com.guitu.repository;

import com.guitu.domain.SupplyDemand;
import com.guitu.domain.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface SupplyDemandRepository extends JpaRepository<SupplyDemand, Long>, JpaSpecificationExecutor<SupplyDemand> {
    Page<SupplyDemand> findByPublisherId(Long publisherId, Pageable pageable);

    long countByStatusIn(Collection<DonationStatus> statuses);
}
