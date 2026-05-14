package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.CommunityComment;
import com.guitu.domain.CommunityPost;
import com.guitu.domain.User;
import com.guitu.domain.enums.CommunityPostStatus;
import com.guitu.domain.enums.UserRole;
import com.guitu.dto.CommunityDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.CommunityCommentRepository;
import com.guitu.repository.CommunityPostRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UserService userService;
    private final DtoMapper mapper;

    public CommunityService(
            CommunityPostRepository communityPostRepository,
            CommunityCommentRepository communityCommentRepository,
            UserService userService,
            DtoMapper mapper
    ) {
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
        this.userService = userService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityDtos.CommunityPostResponse> listPublic(String keyword, int page, int size) {
        Page<CommunityPost> result = communityPostRepository.findAll(publicSpec(keyword), pageRequest(page, size));
        return PageResponse.from(result, post -> mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostId(post.getId())));
    }

    @Transactional(readOnly = true)
    public CommunityDtos.CommunityPostDetailResponse detailPublic(Long id) {
        CommunityPost post = getPublicPost(id);
        return new CommunityDtos.CommunityPostDetailResponse(
                mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostId(post.getId())),
                communityCommentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(mapper::toCommunityCommentResponse)
                        .toList()
        );
    }

    @Transactional
    public CommunityDtos.CommunityPostResponse create(CommunityDtos.SavePostRequest request) {
        User currentUser = userService.currentUser();
        CommunityPost post = new CommunityPost();
        post.setAuthor(currentUser);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setStatus(CommunityPostStatus.PUBLISHED);
        if (request.imageUrls() != null) {
            post.getImageUrls().addAll(request.imageUrls());
        }
        CommunityPost saved = communityPostRepository.save(post);
        return mapper.toCommunityPostResponse(saved, 0);
    }

    @Transactional
    public CommunityDtos.CommunityPostResponse update(Long id, CommunityDtos.SavePostRequest request) {
        CommunityPost post = getManagedPost(id);
        User currentUser = userService.currentUser();
        ensurePostEditable(post, currentUser);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.getImageUrls().clear();
        if (request.imageUrls() != null) {
            post.getImageUrls().addAll(request.imageUrls());
        }
        return mapper.toCommunityPostResponse(post, communityCommentRepository.countByPostId(post.getId()));
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
        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(currentUser);
        comment.setContent(request.content().trim());
        if (request.parentCommentId() != null) {
            CommunityComment parent = communityCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "父评论不存在"));
            comment.setParentComment(parent);
        }
        if (request.imageUrls() != null) {
            comment.getImageUrls().addAll(request.imageUrls());
        }
        return mapper.toCommunityCommentResponse(communityCommentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long id) {
        CommunityComment comment = communityCommentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "评论不存在"));
        User currentUser = userService.currentUser();
        if (!isOwnerOrAdmin(comment.getAuthor().getId(), currentUser)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权限删除该评论");
        }
        communityCommentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public CommunityPost getPublicPost(Long id) {
        CommunityPost post = getManagedPost(id);
        if (post.getStatus() != CommunityPostStatus.PUBLISHED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或已下架");
        }
        return post;
    }

    @Transactional(readOnly = true)
    public CommunityPost getManagedPost(Long id) {
        return communityPostRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "帖子不存在或已下架"));
    }

    private void ensurePostEditable(CommunityPost post, User currentUser) {
        if (post.getStatus() != CommunityPostStatus.PUBLISHED) {
            throw new BusinessException("该帖子已下架，暂不支持继续操作");
        }
        if (!isOwnerOrAdmin(post.getAuthor().getId(), currentUser)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权限操作该帖子");
        }
    }

    private boolean isOwnerOrAdmin(Long ownerId, User currentUser) {
        return ownerId.equals(currentUser.getId()) || currentUser.getRole() == UserRole.ADMIN;
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
