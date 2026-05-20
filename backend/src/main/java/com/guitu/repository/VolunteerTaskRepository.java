package com.guitu.repository;

import com.guitu.domain.VolunteerTask;
import com.guitu.domain.enums.VolunteerTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface VolunteerTaskRepository extends JpaRepository<VolunteerTask, Long>, JpaSpecificationExecutor<VolunteerTask> {
    Page<VolunteerTask> findByPublisherId(Long publisherId, Pageable pageable);

    long countByStatusIn(Collection<VolunteerTaskStatus> statuses);
}
