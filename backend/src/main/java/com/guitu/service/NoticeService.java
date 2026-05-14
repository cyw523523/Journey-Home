package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.Notice;
import com.guitu.domain.User;
import com.guitu.domain.enums.NoticeStatus;
import com.guitu.dto.NoticeDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.NoticeRepository;
import org.springframework.cache.annotation.Cacheable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final ContentModerationService moderationService;
    private final CacheInvalidationService cacheInvalidationService;

    public NoticeService(
            NoticeRepository noticeRepository,
            UserService userService,
            DtoMapper mapper,
            ContentModerationService moderationService,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.noticeRepository = noticeRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.moderationService = moderationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeDtos.NoticeResponse> listPublic(String keyword, int page, int size) {
        Page<Notice> result = noticeRepository.findAll(publicSpec(keyword), pageRequest(page, size));
        return PageResponse.from(result, mapper::toNoticeResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeDtos.NoticeResponse> listAdmin(String keyword, NoticeStatus status, int page, int size) {
        Page<Notice> result = noticeRepository.findAll(adminSpec(keyword, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toNoticeResponse);
    }

    @Cacheable("latestNotices")
    @Transactional(readOnly = true)
    public List<NoticeDtos.NoticeResponse> latestPublic(int size) {
        Page<Notice> result = noticeRepository.findAll(
                publicSpec(null),
                PageRequest.of(0, Math.max(1, size), Sort.by(Sort.Direction.DESC, "publishedAt"))
        );
        return result.getContent().stream().map(mapper::toNoticeResponse).toList();
    }

    @Transactional(readOnly = true)
    public NoticeDtos.NoticeResponse detailPublic(Long id) {
        Notice notice = getEntity(id);
        if (notice.getStatus() != NoticeStatus.PUBLISHED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "公告不存在或已下架");
        }
        return mapper.toNoticeResponse(notice);
    }

    @Transactional(readOnly = true)
    public NoticeDtos.NoticeResponse detailAdmin(Long id) {
        return mapper.toNoticeResponse(getEntity(id));
    }

    @Transactional
    public NoticeDtos.NoticeResponse create(NoticeDtos.SaveNoticeRequest request) {
        User publisher = userService.currentUser();
        moderationService.validateText("Notice content", request.title(), request.content());
        Notice notice = new Notice();
        notice.setPublisher(publisher);
        fillNotice(notice, request);
        NoticeDtos.NoticeResponse response = mapper.toNoticeResponse(noticeRepository.save(notice));
        cacheInvalidationService.evictPublicCaches();
        return response;
    }

    @Transactional
    public NoticeDtos.NoticeResponse update(Long id, NoticeDtos.SaveNoticeRequest request) {
        Notice notice = getEntity(id);
        moderationService.validateText("Notice content", request.title(), request.content());
        fillNotice(notice, request);
        cacheInvalidationService.evictPublicCaches();
        return mapper.toNoticeResponse(notice);
    }

    @Transactional
    public void offline(Long id) {
        Notice notice = getEntity(id);
        notice.setStatus(NoticeStatus.OFFLINE);
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.delete(getEntity(id));
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional(readOnly = true)
    public Notice getEntity(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "公告不存在或已下架"));
    }

    private void fillNotice(Notice notice, NoticeDtos.SaveNoticeRequest request) {
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setStatus(request.status());
        if (request.status() == NoticeStatus.PUBLISHED && notice.getPublishedAt() == null) {
            notice.setPublishedAt(LocalDateTime.now());
        }
        if (request.status() != NoticeStatus.PUBLISHED) {
            notice.setPublishedAt(null);
        }
    }

    private Specification<Notice> publicSpec(String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), NoticeStatus.PUBLISHED));
            addKeyword(keyword, root, cb, predicates);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Notice> adminSpec(String keyword, NoticeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            addKeyword(keyword, root, cb, predicates);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addKeyword(String keyword, jakarta.persistence.criteria.Root<Notice> root, jakarta.persistence.criteria.CriteriaBuilder cb, List<Predicate> predicates) {
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            predicates.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("content"), like)));
        }
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
