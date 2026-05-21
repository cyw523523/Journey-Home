package com.guitu.repository;

import com.guitu.domain.CommunityPostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityPostFavoriteRepository extends JpaRepository<CommunityPostFavorite, Long> {
    Optional<CommunityPostFavorite> findByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    Page<CommunityPostFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
