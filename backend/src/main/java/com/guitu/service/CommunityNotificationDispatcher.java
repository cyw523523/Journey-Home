package com.guitu.service;

import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.repository.CommunityCommentRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.CommunityUserFollowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommunityNotificationDispatcher {

    private final NotificationService notificationService;
    private final CommunityUserFollowRepository followRepo;
    private final CommunityPostRepository postRepo;
    private final CommunityCommentRepository commentRepo;

    private final Map<String, LocalDateTime> dedupCache = new ConcurrentHashMap<>();

    public CommunityNotificationDispatcher(NotificationService notificationService,
            CommunityUserFollowRepository followRepo, CommunityPostRepository postRepo,
            CommunityCommentRepository commentRepo) {
        this.notificationService = notificationService;
        this.followRepo = followRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    private boolean shouldDispatch(String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = dedupCache.get(key);
        if (last != null && java.time.Duration.between(last, now).toMinutes() < 1) {
            return false;
        }
        dedupCache.put(key, now);
        return true;
    }

    public void dispatchPostCommented(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|POST_COMMENTED|" + postId;
        if (!shouldDispatch(key)) return;
        dispatch(toUserId, NotificationType.COMMUNITY_POST_COMMENTED,
                fromUserId + "|" + postId, "COMMUNITY_POST", postId);
    }

    public void dispatchCommentReplied(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|COMMENT_REPLIED|" + postId;
        if (!shouldDispatch(key)) return;
        dispatch(toUserId, NotificationType.COMMUNITY_COMMENT_REPLIED,
                fromUserId + "|" + postId + "|" + commentId, "COMMUNITY_COMMENT", commentId);
    }

    public void dispatchPostLiked(Long toUserId, Long postId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|POST_LIKED|" + postId;
        if (!shouldDispatch(key)) return;
        dispatch(toUserId, NotificationType.COMMUNITY_POST_LIKED,
                postId.toString(), "COMMUNITY_POST", postId);
    }

    public void dispatchCommentLiked(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|COMMENT_LIKED|" + commentId;
        if (!shouldDispatch(key)) return;
        dispatch(toUserId, NotificationType.COMMUNITY_COMMENT_LIKED,
                postId + "|" + commentId, "COMMUNITY_COMMENT", commentId);
    }

    public void dispatchMentioned(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|MENTIONED|" + postId;
        if (!shouldDispatch(key)) return;
        dispatch(toUserId, NotificationType.COMMUNITY_MENTIONED,
                postId + "|" + commentId, "COMMUNITY_COMMENT", commentId);
    }

    @Async
    public void broadcastNewPost(Long authorId, Long postId) {
        long followerCount = followRepo.countByFolloweeId(authorId);
        if (followerCount == 0) return;
        int limit = (int) Math.min(followerCount, 200);
        Pageable pageable = PageRequest.of(0, limit);
        List<Long> followerIds = followRepo.findByFolloweeIdOrderByCreatedAtDesc(authorId, pageable)
                .stream().map(f -> f.getFollower().getId()).toList();
        for (Long fid : followerIds) {
            String key = fid + "|" + authorId + "|FOLLOWED_POST|" + postId;
            if (!shouldDispatch(key)) continue;
            dispatch(fid, NotificationType.COMMUNITY_FOLLOWED_NEW_POST,
                    authorId + "|" + postId, "COMMUNITY_POST", postId);
        }
    }

    private void dispatch(Long toUserId, NotificationType type, String content,
            String relatedTargetType, Long relatedTargetId) {
        User u = new User();
        u.setId(toUserId);
        notificationService.notifyUser(u, type, type.name(), content,
                relatedTargetType, relatedTargetId);
    }
}
