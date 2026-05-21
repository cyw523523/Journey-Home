package com.guitu.repository;

import com.guitu.domain.CommunityCommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentMentionRepository extends JpaRepository<CommunityCommentMention, Long> {
    List<CommunityCommentMention> findByCommentId(Long commentId);
}
