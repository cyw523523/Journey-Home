package com.guitu.repository;

import com.guitu.domain.CommunityUserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityUserFollowRepository extends JpaRepository<CommunityUserFollow, Long> {
    Optional<CommunityUserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Page<CommunityUserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);
    Page<CommunityUserFollow> findByFolloweeIdOrderByCreatedAtDesc(Long followeeId, Pageable pageable);
    long countByFolloweeId(Long followeeId);
    long countByFollowerId(Long followerId);
}
