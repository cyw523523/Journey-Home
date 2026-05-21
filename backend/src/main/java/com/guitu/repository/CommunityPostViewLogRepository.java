package com.guitu.repository;

import com.guitu.domain.CommunityPostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface CommunityPostViewLogRepository extends JpaRepository<CommunityPostViewLog, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO community_post_view_logs (post_id, viewer_key, viewed_on, created_at, updated_at) VALUES (?1, ?2, ?3, NOW(), NOW())", nativeQuery = true)
    int insertIgnore(Long postId, String viewerKey, LocalDate viewedOn);
}
