package com.guitu.repository;

import com.guitu.domain.CommunityComment;
import com.guitu.domain.enums.CommunityCommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, JpaSpecificationExecutor<CommunityComment> {
    List<CommunityComment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommunityCommentStatus status);

    long countByPostId(Long postId);

    long countByPostIdAndStatus(Long postId, CommunityCommentStatus status);

    // Floor pagination (only comments with floor_no IS NOT NULL)
    Page<CommunityComment> findByPostIdAndFloorNoIsNotNullOrderByFloorNoAsc(Long postId, Pageable pageable);
    Page<CommunityComment> findByPostIdAndFloorNoIsNotNullOrderByFloorNoDesc(Long postId, Pageable pageable);
    Page<CommunityComment> findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoAsc(Long postId, Long authorId, Pageable pageable);
    Page<CommunityComment> findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoDesc(Long postId, Long authorId, Pageable pageable);

    // Max floor number for a post
    @Query("SELECT COALESCE(MAX(c.floorNo), 0) FROM CommunityComment c WHERE c.post.id = :postId AND c.floorNo IS NOT NULL")
    int maxFloorNo(@Param("postId") Long postId);

    // Sub-replies for a floor (root comment)
    Page<CommunityComment> findByRootCommentIdOrderByCreatedAtAsc(Long rootCommentId, Pageable pageable);

    // Count of sub-replies
    int countByRootCommentId(Long rootCommentId);

    // Top 3 sub-replies for each floor (used in floor list)
    List<CommunityComment> findTop3ByRootCommentIdOrderByCreatedAtAsc(Long rootCommentId);
}
