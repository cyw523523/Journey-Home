package com.guitu.repository;

import com.guitu.domain.RescueStation;
import com.guitu.domain.enums.CertificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RescueStationRepository extends JpaRepository<RescueStation, Long>, JpaSpecificationExecutor<RescueStation> {
    Optional<RescueStation> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByCertificationStatus(CertificationStatus status);
}
