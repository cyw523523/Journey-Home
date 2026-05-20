package com.guitu.repository;

import com.guitu.domain.DonationRecord;
import com.guitu.domain.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DonationRecordRepository extends JpaRepository<DonationRecord, Long>, JpaSpecificationExecutor<DonationRecord> {
    Page<DonationRecord> findByDonorIdOrderByCreatedAtDesc(Long donorId, Pageable pageable);

    Page<DonationRecord> findByDemandIdOrderByCreatedAtDesc(Long demandId, Pageable pageable);

    List<DonationRecord> findByDemandIdAndStatus(Long demandId, DonationStatus status);

    long countByDonorIdAndStatus(Long donorId, DonationStatus status);
}
