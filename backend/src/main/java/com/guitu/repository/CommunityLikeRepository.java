package com.guitu.repository;

import com.guitu.domain.CommunityLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {
    Optional<CommunityLike> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);

    int countByTargetTypeAndTargetId(String targetType, Long targetId);

    List<CommunityLike> findByTargetTypeAndTargetIdIn(String targetType, List<Long> targetIds);

    @Modifying
    @Query("DELETE FROM CommunityLike l WHERE l.targetType = :targetType AND l.targetId = :targetId")
    void deleteByTarget(String targetType, Long targetId);
}
