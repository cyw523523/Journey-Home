package com.guitu.repository;

import com.guitu.domain.CommunityComment;
import com.guitu.domain.enums.CommunityCommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, JpaSpecificationExecutor<CommunityComment> {
    List<CommunityComment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommunityCommentStatus status);

    long countByPostId(Long postId);

    long countByPostIdAndStatus(Long postId, CommunityCommentStatus status);
}
