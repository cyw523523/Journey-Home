package com.guitu.service;

import com.guitu.domain.CommunityUserFollow;
import com.guitu.domain.User;
import com.guitu.exception.BusinessException;
import com.guitu.repository.CommunityUserFollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityFollowService {
    private final CommunityUserFollowRepository repo;

    public CommunityFollowService(CommunityUserFollowRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) throw new BusinessException("不能关注自己");
        if (repo.existsByFollowerIdAndFolloweeId(followerId, followeeId)) return;
        CommunityUserFollow f = new CommunityUserFollow();
        User follower = new User();
        follower.setId(followerId);
        f.setFollower(follower);
        User followee = new User();
        followee.setId(followeeId);
        f.setFollowee(followee);
        repo.save(f);
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        repo.findByFollowerIdAndFolloweeId(followerId, followeeId).ifPresent(repo::delete);
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        return followerId != null && repo.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    public Page<CommunityUserFollow> listFollowing(Long userId, Pageable pageable) {
        return repo.findByFollowerIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<CommunityUserFollow> listFollowers(Long userId, Pageable pageable) {
        return repo.findByFolloweeIdOrderByCreatedAtDesc(userId, pageable);
    }
}
