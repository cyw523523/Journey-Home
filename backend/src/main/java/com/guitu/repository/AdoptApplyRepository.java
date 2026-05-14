package com.guitu.repository;

import com.guitu.domain.AdoptApply;
import com.guitu.domain.enums.ApplyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdoptApplyRepository extends JpaRepository<AdoptApply, Long>, JpaSpecificationExecutor<AdoptApply> {
    Page<AdoptApply> findByApplicantId(Long applicantId, Pageable pageable);

    boolean existsByAnimalIdAndApplicantIdAndStatus(Long animalId, Long applicantId, ApplyStatus status);

    long countByAnimalId(Long animalId);

    long countByAnimalIdAndStatus(Long animalId, ApplyStatus status);

    long countByStatus(ApplyStatus status);

    List<AdoptApply> findByAnimalIdAndStatusAndIdNot(Long animalId, ApplyStatus status, Long id);

    @Query("select a.status, count(a) from AdoptApply a group by a.status")
    List<Object[]> countGroupByStatus();
}
