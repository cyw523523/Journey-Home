package com.guitu.service;

import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.repository.CommunityCommentRepository;
import com.guitu.repository.CommunityPostRepository;
import com.guitu.repository.CommunityUserFollowRepository;
import com.guitu.repository.UserRepository;
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
    private final UserRepository userRepo;

    private final Map<String, LocalDateTime> dedupCache = new ConcurrentHashMap<>();

    public CommunityNotificationDispatcher(NotificationService notificationService,
            CommunityUserFollowRepository followRepo, CommunityPostRepository postRepo,
            CommunityCommentRepository commentRepo, UserRepository userRepo) {
        this.notificationService = notificationService;
        this.followRepo = followRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
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

    private String nickname(Long userId) {
        return userRepo.findById(userId).map(User::getNickname).orElse("用户" + userId);
    }

    private String postTitle(Long postId) {
        return postRepo.findById(postId).map(CommunityPost::getTitle).orElse("帖子" + postId);
    }

    public void dispatchPostCommented(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|POST_COMMENTED|" + postId;
        if (!shouldDispatch(key)) return;
        String content = nickname(fromUserId) + " 评论了你的帖子「" + postTitle(postId) + "」";
        dispatch(toUserId, NotificationType.COMMUNITY_POST_COMMENTED,
                fromUserId + "|" + postId, "COMMUNITY_POST", postId, content);
    }

    public void dispatchCommentReplied(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|COMMENT_REPLIED|" + postId;
        if (!shouldDispatch(key)) return;
        String content = nickname(fromUserId) + " 回复了你在帖子「" + postTitle(postId) + "」中的评论";
        dispatch(toUserId, NotificationType.COMMUNITY_COMMENT_REPLIED,
                fromUserId + "|" + postId + "|" + commentId, "COMMUNITY_COMMENT", commentId, content);
    }

    public void dispatchPostLiked(Long toUserId, Long postId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|POST_LIKED|" + postId;
        if (!shouldDispatch(key)) return;
        String content = nickname(fromUserId) + " 赞了你的帖子「" + postTitle(postId) + "」";
        dispatch(toUserId, NotificationType.COMMUNITY_POST_LIKED,
                postId.toString(), "COMMUNITY_POST", postId, content);
    }

    public void dispatchCommentLiked(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|COMMENT_LIKED|" + commentId;
        if (!shouldDispatch(key)) return;
        String content = nickname(fromUserId) + " 赞了你在帖子「" + postTitle(postId) + "」中的评论";
        dispatch(toUserId, NotificationType.COMMUNITY_COMMENT_LIKED,
                postId + "|" + commentId, "COMMUNITY_COMMENT", commentId, content);
    }

    public void dispatchMentioned(Long toUserId, Long postId, Long commentId, Long fromUserId) {
        String key = toUserId + "|" + fromUserId + "|MENTIONED|" + postId;
        if (!shouldDispatch(key)) return;
        String content = nickname(fromUserId) + " 在帖子「" + postTitle(postId) + "」中 @ 了你";
        dispatch(toUserId, NotificationType.COMMUNITY_MENTIONED,
                postId + "|" + commentId, "COMMUNITY_COMMENT", commentId, content);
    }

    @Async
    public void broadcastNewPost(Long authorId, Long postId) {
        long followerCount = followRepo.countByFolloweeId(authorId);
        if (followerCount == 0) return;
        int limit = (int) Math.min(followerCount, 200);
        Pageable pageable = PageRequest.of(0, limit);
        List<Long> followerIds = followRepo.findByFolloweeIdOrderByCreatedAtDesc(authorId, pageable)
                .stream().map(f -> f.getFollower().getId()).toList();
        String content = nickname(authorId) + " 发布了新帖子「" + postTitle(postId) + "」";
        for (Long fid : followerIds) {
            String key = fid + "|" + authorId + "|FOLLOWED_POST|" + postId;
            if (!shouldDispatch(key)) continue;
            dispatch(fid, NotificationType.COMMUNITY_FOLLOWED_NEW_POST,
                    authorId + "|" + postId, "COMMUNITY_POST", postId, content);
        }
    }

    private void dispatch(Long toUserId, NotificationType type, String idContent,
            String relatedTargetType, Long relatedTargetId, String readableContent) {
        User u = new User();
        u.setId(toUserId);
        notificationService.notifyUser(u, type, type.name(), readableContent != null ? readableContent : idContent,
                relatedTargetType, relatedTargetId);
    }

    private void dispatch(Long toUserId, NotificationType type, String content,
            String relatedTargetType, Long relatedTargetId) {
        dispatch(toUserId, type, content, relatedTargetType, relatedTargetId, null);
    }
}
