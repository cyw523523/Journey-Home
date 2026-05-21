package com.guitu.repository;

import com.guitu.domain.CommunityPost;
import com.guitu.domain.enums.CommunityPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, JpaSpecificationExecutor<CommunityPost> {
    Page<CommunityPost> findByAuthorIdInAndStatusOrderByLastActiveAtDesc(List<Long> authorIds, CommunityPostStatus status, Pageable pageable);
}
