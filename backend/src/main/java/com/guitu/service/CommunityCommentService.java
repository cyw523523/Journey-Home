package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.*;
import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.dto.CommunityDtos.*;
import com.guitu.exception.BusinessException;
import com.guitu.repository.*;
import com.guitu.security.SecurityPrincipal;
import com.guitu.security.SecuritySupport;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommunityCommentService {
    private final CommunityCommentRepository commentRepo;
    private final CommunityPostRepository postRepo;
    private final CommunityLikeService likeService;

    public CommunityCommentService(CommunityCommentRepository commentRepo,
            CommunityPostRepository postRepo, CommunityLikeService likeService) {
        this.commentRepo = commentRepo;
        this.postRepo = postRepo;
        this.likeService = likeService;
    }

    @Transactional
    public FloorResponse createFloor(Long postId, SaveFloorRequest req) {
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new BusinessException("帖子不存在"));
        SecurityPrincipal principal = SecuritySupport.currentUser()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录"));

        int maxFloor = commentRepo.maxFloorNo(postId);
        int newFloor = maxFloor + 1;

        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(userRef(principal.id()));
        comment.setContent(req.content());
        comment.setImageUrls(req.imageUrls() != null ? req.imageUrls() : List.of());
        comment.setFloorNo(newFloor);
        comment.setStatus(CommunityCommentStatus.PUBLISHED);
        commentRepo.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        post.setLastActiveAt(LocalDateTime.now());
        postRepo.save(post);

        return toFloorResponseFull(comment, post.getAuthor().getId(), principal.id(), List.of());
    }

    @Transactional
    public ReplyResponse createReply(Long floorId, SaveReplyRequest req) {
        CommunityComment floor = commentRepo.findById(floorId)
                .orElseThrow(() -> new BusinessException("楼层不存在"));
        SecurityPrincipal principal = SecuritySupport.currentUser()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录"));
        CommunityPost post = floor.getPost();

        CommunityComment replyTo = commentRepo.findById(req.replyToCommentId())
                .orElseThrow(() -> new BusinessException("回复目标不存在"));

        // Determine root: if replyTo IS a floor, that's root; otherwise use replyTo's root
        Long rootId = replyTo.getFloorNo() != null ? replyTo.getId() : replyTo.getRootComment().getId();

        CommunityComment reply = new CommunityComment();
        reply.setPost(post);
        reply.setAuthor(userRef(principal.id()));
        reply.setContent(req.content());
        reply.setImageUrls(req.imageUrls() != null ? req.imageUrls() : List.of());
        reply.setFloorNo(null);

        CommunityComment rootRef = new CommunityComment();
        rootRef.setId(rootId);
        reply.setRootComment(rootRef);
        reply.setReplyToComment(replyTo);
        reply.setStatus(CommunityCommentStatus.PUBLISHED);
        commentRepo.save(reply);

        post.setCommentCount(post.getCommentCount() + 1);
        post.setLastActiveAt(LocalDateTime.now());
        postRepo.save(post);

        // Parse mentions (will be enhanced in Task 12)
        List<MentionInfo> mentions = List.of();

        return toReplyResponse(reply, mentions, principal.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<FloorResponse> listFloors(Long postId, Long currentUserId, boolean onlyAuthor,
            boolean desc, Long postAuthorId, int page, int size) {
        Page<CommunityComment> floorPage;
        if (onlyAuthor && postAuthorId != null) {
            floorPage = desc
                    ? commentRepo.findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoDesc(
                            postId, postAuthorId, PageRequest.of(page, size))
                    : commentRepo.findByPostIdAndAuthorIdAndFloorNoIsNotNullOrderByFloorNoAsc(
                            postId, postAuthorId, PageRequest.of(page, size));
        } else {
            floorPage = desc
                    ? commentRepo.findByPostIdAndFloorNoIsNotNullOrderByFloorNoDesc(
                            postId, PageRequest.of(page, size))
                    : commentRepo.findByPostIdAndFloorNoIsNotNullOrderByFloorNoAsc(
                            postId, PageRequest.of(page, size));
        }

        List<Long> floorIds = floorPage.stream().map(BaseEntity::getId).toList();
        List<Long> likedIds = currentUserId != null
                ? likeService.getLikedTargetIds(currentUserId, "COMMENT", floorIds)
                : List.of();

        List<Long> finalLikedIds = likedIds;
        return PageResponse.from(floorPage, floor -> toFloorResponseFull(floor, postAuthorId, currentUserId, finalLikedIds));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReplyResponse> listReplies(Long floorId, Long currentUserId, int page, int size) {
        Page<CommunityComment> replies = commentRepo.findByRootCommentIdOrderByCreatedAtAsc(
                floorId, PageRequest.of(page, size));
        return PageResponse.from(replies, r -> toReplyResponse(r, List.of(), currentUserId));
    }

    @Transactional
    public void deleteComment(Long id) {
        CommunityComment comment = commentRepo.findById(id)
                .orElseThrow(() -> new BusinessException("评论不存在"));
        comment.setStatus(CommunityCommentStatus.DELETED);
        comment.setContent("该评论已删除");
        commentRepo.save(comment);
        comment.getPost().setCommentCount(Math.max(0, comment.getPost().getCommentCount() - 1));
        postRepo.save(comment.getPost());
    }

    @Transactional(readOnly = true)
    public CommunityComment getFloor(Long id) {
        return commentRepo.findById(id)
                .orElseThrow(() -> new BusinessException("楼层不存在"));
    }

    // --- Private helpers ---

    private FloorResponse toFloorResponseFull(CommunityComment c, Long postAuthorId, Long currentUserId,
            List<Long> likedIds) {
        List<CommunityComment> topReplies = commentRepo.findTop3ByRootCommentIdOrderByCreatedAtAsc(c.getId());
        int replyCount = commentRepo.countByRootCommentId(c.getId());
        boolean liked = likedIds.contains(c.getId());
        boolean isPostAuthor = c.getAuthor().getId().equals(postAuthorId);

        return new FloorResponse(c.getId(), c.getFloorNo(), effectiveContent(c),
                c.getImageUrls(), c.getAuthor().getId(), c.getAuthor().getNickname(),
                c.getAuthor().getAvatarUrl(), c.getAuthor().getRole().getLabel(),
                c.getCreatedAt(), c.getStatus().name(), c.getLikeCount(), liked, isPostAuthor,
                replyCount,
                topReplies.stream()
                        .map(r -> toReplyResponse(r, List.of(), currentUserId))
                        .toList());
    }

    private ReplyResponse toReplyResponse(CommunityComment r, List<MentionInfo> mentions,
            Long currentUserId) {
        boolean liked = currentUserId != null
                && likeService.isLiked(currentUserId, "COMMENT", r.getId());
        String replyToNickname = r.getReplyToComment() != null
                && r.getReplyToComment().getAuthor() != null
                        ? r.getReplyToComment().getAuthor().getNickname()
                        : null;
        Long replyToUserId = r.getReplyToComment() != null
                && r.getReplyToComment().getAuthor() != null
                        ? r.getReplyToComment().getAuthor().getId()
                        : null;
        return new ReplyResponse(r.getId(), effectiveContent(r), r.getImageUrls(),
                r.getAuthor().getId(), r.getAuthor().getNickname(), r.getAuthor().getAvatarUrl(),
                r.getAuthor().getRole().getLabel(), replyToUserId, replyToNickname,
                r.getCreatedAt(), r.getStatus().name(), r.getLikeCount(), liked, mentions);
    }

    private String effectiveContent(CommunityComment c) {
        return c.getStatus() == CommunityCommentStatus.DELETED ? "该评论已删除" : c.getContent();
    }

    private User userRef(Long userId) {
        User u = new User();
        u.setId(userId);
        return u;
    }
}
