package com.guitu.repository;

import com.guitu.domain.AdoptionFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdoptionFollowUpRepository extends JpaRepository<AdoptionFollowUp, Long> {
    List<AdoptionFollowUp> findByApplyIdOrderByPlannedAtAsc(Long applyId);

    boolean existsByApplyId(Long applyId);
}
