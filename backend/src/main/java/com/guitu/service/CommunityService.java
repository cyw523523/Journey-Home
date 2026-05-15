package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.User;
import com.guitu.domain.enums.CommunityCommentStatus;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.UserRole;
import com.guitu.dto.CommunityDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityService {
    private final com.guitu.repository.CommunityPostRepository communityPostRepository;
    private final com.guitu.repository.CommunityCommentRepository communityCommentRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final AntiAbuseService antiAbuseService;
    private final ContentModerationService moderationService;
    private final NotificationService notificationService;

    public CommunityService(
            com.guitu.repository.CommunityPostRepository communityPostRepository,
            com.guitu.repository.CommunityCommentRepository communityCommentRepository,
            UserService userService,
            DtoMapper mapper,
            AntiAbuseService antiAbuseService,
            ContentModerationService moderationService,
            NotificationService notificationService
    ) {
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.antiAbuseService = antiAbuseService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDtos.CommunityPostResponse> listPublic(String keyword, int page, int size) {
        Page<CommunityPost> result = communityPostRepository.findAll(publicSpec(keyword), pageRequest(page, size));
        return PageResponse.from(result, post ->
                mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostIdAndStatus(post.getId(), CommunityCommentStatus.PUBLISHED))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDtos.CommunityPostResponse> listMine(int page, int size) {
        Long userId = userService.currentUser().getId();
        Page<CommunityPost> result = communityPostRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("author").get("id"), userId),
                pageRequest(page, size)
        );
        return PageResponse.from(result, post ->
                mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostIdAndStatus(post.getId(), CommunityCommentStatus.PUBLISHED))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDtos.CommunityCommentResponse> listMyComments(int page, int size) {
        Long userId = userService.currentUser().getId();
        Page<CommunityComment> result = communityCommentRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("author").get("id"), userId),
                pageRequest(page, size)
        );
        return PageResponse.from(result, mapper::toCommunityCommentResponse);
    }

    @Transactional(readOnly = true)
    public CommunityDtos.CommunityPostDetailResponse detailPublic(Long id) {
        CommunityPost post = getPublicPost(id);
        return new CommunityDtos.CommunityPostDetailResponse(
                mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostIdAndStatus(post.getId(), CommunityCommentStatus.PUBLISHED)),
                communityCommentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), CommunityCommentStatus.PUBLISHED).stream()
                        .map(mapper::toCommunityCommentResponse)
                        .toList()
        );
    }

    @Transactional
    public CommunityDtos.CommunityPostResponse create(CommunityDtos.SavePostRequest request) {
        User currentUser = userService.currentUser();
        antiAbuseService.check("community-post", currentUser.getId().toString(), 5, Duration.ofMinutes(10), "Too many post submissions, please try again later");
        moderationService.validateText("Post content", request.title(), request.content());

        CommunityPost post = new CommunityPost();
        post.setAuthor(currentUser);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setStatus(resolvePostStatus(false, request));
        post.getImageUrls().clear();
        if (request.imageUrls() != null) {
            post.getImageUrls().addAll(request.imageUrls());
        }
        CommunityPost saved = communityPostRepository.save(post);
        notifyPostReview(saved);
        return mapper.toCommunityPostResponse(saved, 0);
    }

    @Transactional
    public CommunityDtos.CommunityPostResponse update(Long id, CommunityDtos.SavePostRequest request) {
        CommunityPost post = getManagedPost(id);
        User currentUser = userService.currentUser();
        ensurePostEditable(post, currentUser);
        antiAbuseService.check("community-post-edit", currentUser.getId().toString(), 8, Duration.ofMinutes(10), "Too many post edits, please try again later");
        moderationService.validateText("Post content", request.title(), request.content());

        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.getImageUrls().clear();
        if (request.imageUrls() != null) {
            post.getImageUrls().addAll(request.imageUrls());
        }
        post.setStatus(resolvePostStatus(currentUser.getRole() == UserRole.ADMIN, request));
        notifyPostReview(post);
        if (currentUser.getRole() == UserRole.ADMIN && !post.getAuthor().getId().equals(currentUser.getId())) {
            notificationService.notifyUser(post.getAuthor(), NotificationType.AUDIT_RESULT,
                    "ADMIN_EDIT_POST", "管理员编辑了你的社区帖子「" + post.getTitle() + "」",
                    "COMMUNITY_POST", post.getId());
        }
        return mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostIdAndStatus(post.getId(), CommunityCommentStatus.PUBLISHED));
    }

    @Transactional
    public void delete(Long id) {
        CommunityPost post = getManagedPost(id);
        User currentUser = userService.currentUser();
        ensurePostEditable(post, currentUser);
        post.setStatus(CommunityPostStatus.OFFLINE);
    }

    @Transactional
    public CommunityDtos.CommunityCommentResponse addComment(Long postId, CommunityDtos.SaveCommentRequest request) {
        CommunityPost post = getPublicPost(postId);
        User currentUser = userService.currentUser();
        antiAbuseService.check("community-comment", currentUser.getId().toString(), 12, Duration.ofMinutes(10), "Too many comments, please try again later");
        moderationService.validateText("Comment content", request.content());

        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(currentUser);
        comment.setContent(request.content().trim());
        comment.setStatus(resolveCommentStatus(false, request));
        CommunityComment parent = null;
        if (request.parentCommentId() != null) {
            parent = getManagedComment(request.parentCommentId());
            comment.setParentComment(parent);
        }
        if (request.imageUrls() != null) {
            comment.getImageUrls().addAll(request.imageUrls());
        }
        CommunityComment saved = communityCommentRepository.save(comment);
        notifyCommentReview(saved);
        notifyReply(saved, post, parent, currentUser);
        return mapper.toCommunityCommentResponse(saved);
    }

    @Transactional
    public void deleteComment(Long id) {
        CommunityComment comment = getManagedComment(id);
        User currentUser = userService.currentUser();
        if (!isOwnerOrAdmin(comment.getAuthor().getId(), currentUser)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot delete this comment");
        }
        comment.setStatus(CommunityCommentStatus.OFFLINE);
        if (currentUser.getRole() == UserRole.ADMIN && !comment.getAuthor().getId().equals(currentUser.getId())) {
            notificationService.notifyUser(comment.getAuthor(), NotificationType.AUDIT_RESULT,
                    "ADMIN_DELETE_COMMENT", "管理员删除了你在帖子中的一条评论",
                    "COMMUNITY_COMMENT", comment.getId());
        }
    }

    @Transactional(readOnly = true)
    public CommunityPost getPublicPost(Long id) {
        CommunityPost post = getManagedPost(id);
        if (post.getStatus() != CommunityPostStatus.PUBLISHED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Community post is not available");
        }
        return post;
    }

    @Transactional(readOnly = true)
    public CommunityPost getManagedPost(Long id) {
        return communityPostRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Community post does not exist"));
    }

    @Transactional(readOnly = true)
    public CommunityComment getManagedComment(Long id) {
        return communityCommentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Community comment does not exist"));
    }

    private CommunityPostStatus resolvePostStatus(boolean isAdmin, CommunityDtos.SavePostRequest request) {
        if (isAdmin) {
            return CommunityPostStatus.PUBLISHED;
        }
        return moderationService.requiresManualReview(request.imageUrls(), request.title(), request.content())
                ? CommunityPostStatus.PENDING_REVIEW
                : CommunityPostStatus.PUBLISHED;
    }

    private CommunityCommentStatus resolveCommentStatus(boolean isAdmin, CommunityDtos.SaveCommentRequest request) {
        if (isAdmin) {
            return CommunityCommentStatus.PUBLISHED;
        }
        return moderationService.requiresManualReview(request.imageUrls(), request.content())
                ? CommunityCommentStatus.PENDING_REVIEW
                : CommunityCommentStatus.PUBLISHED;
    }

    private void ensurePostEditable(CommunityPost post, User currentUser) {
        if (post.getStatus() == CommunityPostStatus.OFFLINE) {
            throw new BusinessException("This post is offline and cannot be edited");
        }
        if (!isOwnerOrAdmin(post.getAuthor().getId(), currentUser)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot edit this post");
        }
    }

    private boolean isOwnerOrAdmin(Long ownerId, User currentUser) {
        return ownerId.equals(currentUser.getId()) || currentUser.getRole() == UserRole.ADMIN;
    }

    private void notifyPostReview(CommunityPost post) {
        if (post.getStatus() == CommunityPostStatus.PENDING_REVIEW) {
            notificationService.notifyUser(
                    post.getAuthor(),
                    NotificationType.AUDIT_RESULT,
                    "帖子待审核",
                    "您的帖子包含图片或敏感内容，已提交管理员审核队列。",
                    "COMMUNITY_POST",
                    post.getId()
            );
            notificationService.notifyAdmins(
                    NotificationType.REPORT_CREATED,
                    "新帖子待审核",
                    "有新的社区帖子需要人工审核后才能发布。",
                    "COMMUNITY_POST",
                    post.getId()
            );
        }
    }

    private void notifyCommentReview(CommunityComment comment) {
        if (comment.getStatus() == CommunityCommentStatus.PENDING_REVIEW) {
            notificationService.notifyUser(
                    comment.getAuthor(),
                    NotificationType.AUDIT_RESULT,
                    "评论待审核",
                    "您的评论包含图片或敏感内容，已提交管理员审核队列。",
                    "COMMUNITY_COMMENT",
                    comment.getId()
            );
            notificationService.notifyAdmins(
                    NotificationType.REPORT_CREATED,
                    "新评论待审核",
                    "有新的社区评论需要人工审核后才能发布。",
                    "COMMUNITY_COMMENT",
                    comment.getId()
            );
        }
    }

    private void notifyReply(CommunityComment comment, CommunityPost post, CommunityComment parent, User currentUser) {
        // 只通知已发布的评论
        if (comment.getStatus() != CommunityCommentStatus.PUBLISHED) {
            return;
        }
        // 回复评论：通知父评论作者
        if (parent != null) {
            User parentAuthor = parent.getAuthor();
            if (!parentAuthor.getId().equals(currentUser.getId())) {
                String snippet = comment.getContent().length() > 50 ? comment.getContent().substring(0, 50) + "..." : comment.getContent();
                notificationService.notifyUser(
                        parentAuthor,
                        NotificationType.COMMENT_REPLY,
                        "COMMENT_REPLY_COMMENT",
                        currentUser.getNickname() + "|" + snippet,
                        "COMMUNITY_COMMENT",
                        comment.getId()
                );
            }
        } else {
            // 回复帖子：通知帖子作者
            User postAuthor = post.getAuthor();
            if (!postAuthor.getId().equals(currentUser.getId())) {
                String snippet = comment.getContent().length() > 50 ? comment.getContent().substring(0, 50) + "..." : comment.getContent();
                notificationService.notifyUser(
                        postAuthor,
                        NotificationType.COMMENT_REPLY,
                        "COMMENT_REPLY_POST",
                        currentUser.getNickname() + "|" + snippet,
                        "COMMUNITY_POST",
                        post.getId()
                );
            }
        }
    }

    private Specification<CommunityPost> publicSpec(String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), CommunityPostStatus.PUBLISHED));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("content"), like),
                        cb.like(root.join("author").get("nickname"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
