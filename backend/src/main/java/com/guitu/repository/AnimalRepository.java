package com.guitu.repository;

import com.guitu.domain.Animal;
import com.guitu.domain.enums.AnimalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {
    Page<Animal> findByPublisherId(Long publisherId, Pageable pageable);

    long countByStatusIn(Collection<AnimalStatus> statuses);

    @Query("select a.status, count(a) from Animal a group by a.status")
    List<Object[]> countGroupByStatus();
}
