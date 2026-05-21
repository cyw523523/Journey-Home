package com.guitu.repository;

import com.guitu.domain.CommunityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityCategoryRepository extends JpaRepository<CommunityCategory, Long> {
    List<CommunityCategory> findByEnabledTrueOrderBySortOrderAsc();
    Optional<CommunityCategory> findByCode(String code);
    boolean existsByCode(String code);
}
