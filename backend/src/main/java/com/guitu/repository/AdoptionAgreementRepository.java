package com.guitu.repository;

import com.guitu.domain.AdoptionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdoptionAgreementRepository extends JpaRepository<AdoptionAgreement, Long> {
    Optional<AdoptionAgreement> findByApplyId(Long applyId);

    boolean existsByApplyId(Long applyId);
}
