package com.guitu.service;

import com.guitu.domain.CommunityLike;
import com.guitu.domain.User;
import com.guitu.repository.CommunityLikeRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.CommunityCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommunityLikeService {
    private final CommunityLikeRepository repo;
    private final CommunityPostRepository postRepo;
    private final CommunityCommentRepository commentRepo;

    public CommunityLikeService(CommunityLikeRepository repo, CommunityPostRepository postRepo,
                                CommunityCommentRepository commentRepo) {
        this.repo = repo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    @Transactional
    public boolean toggle(Long userId, String targetType, Long targetId) {
        return repo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
            .map(like -> {
                repo.delete(like);
                updateCount(targetType, targetId, -1);
                return false;
            })
            .orElseGet(() -> {
                CommunityLike like = new CommunityLike();
                User u = new User();
                u.setId(userId);
                like.setUser(u);
                like.setTargetType(targetType);
                like.setTargetId(targetId);
                repo.save(like);
                updateCount(targetType, targetId, 1);
                return true;
            });
    }

    public boolean isLiked(Long userId, String targetType, Long targetId) {
        return repo.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    public List<Long> getLikedTargetIds(Long userId, String targetType, List<Long> targetIds) {
        return repo.findByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
            .filter(l -> l.getUser().getId().equals(userId))
            .map(CommunityLike::getTargetId)
            .toList();
    }

    private void updateCount(String targetType, Long targetId, int delta) {
        if ("POST".equals(targetType)) {
            postRepo.findById(targetId).ifPresent(p -> {
                p.setLikeCount(p.getLikeCount() + delta);
                postRepo.save(p);
            });
        } else {
            commentRepo.findById(targetId).ifPresent(c -> {
                c.setLikeCount(c.getLikeCount() + delta);
                commentRepo.save(c);
            });
        }
    }
}
