package com.guitu.repository;

import com.guitu.domain.Rescue;
import com.guitu.domain.enums.RescueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RescueRepository extends JpaRepository<Rescue, Long>, JpaSpecificationExecutor<Rescue> {
    Page<Rescue> findByPublisherId(Long publisherId, Pageable pageable);

    long countByStatusIn(Collection<RescueStatus> statuses);

    @Query("select r.status, count(r) from Rescue r group by r.status")
    List<Object[]> countGroupByStatus();
}
