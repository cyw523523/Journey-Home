package com.guitu.repository;

import com.guitu.domain.VolunteerApplication;
import com.guitu.domain.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VolunteerApplicationRepository extends JpaRepository<VolunteerApplication, Long>, JpaSpecificationExecutor<VolunteerApplication> {
    Page<VolunteerApplication>findByVolunteerIdOrderByCreatedAtDesc(Long volunteerId, Pageable pageable);

    Page<VolunteerApplication> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);

    long countByTaskIdAndStatus(Long taskId, ApplicationStatus status);

    long countByVolunteerIdAndTaskIdAndStatus(Long volunteerId, Long taskId, ApplicationStatus status);
}
