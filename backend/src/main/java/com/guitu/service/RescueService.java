package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.Rescue;
import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.domain.enums.UserRole;
import com.guitu.dto.RescueDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.RescueRepository;
import com.guitu.security.SecuritySupport;
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

import java.util.ArrayList;
import java.util.List;

@Service
public class RescueService {
    private final RescueRepository rescueRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final ContentModerationService moderationService;
    private final CacheInvalidationService cacheInvalidationService;
    private final NotificationService notificationService;

    public RescueService(
            RescueRepository rescueRepository,
            UserService userService,
            DtoMapper mapper,
            ContentModerationService moderationService,
            CacheInvalidationService cacheInvalidationService,
            NotificationService notificationService
    ) {
        this.rescueRepository = rescueRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.moderationService = moderationService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueDtos.RescueResponse> listPublic(String keyword, String region, RescueStatus status, int page, int size) {
        Page<Rescue> result = rescueRepository.findAll(publicSpec(keyword, region, status), pageRequest(page, size));
        boolean loggedIn = SecuritySupport.currentUser().isPresent();
        return PageResponse.from(result, rescue -> {
            RescueDtos.RescueResponse response = mapper.toRescueResponse(rescue);
            return loggedIn ? response : toPublicResponse(response);
        });
    }

    @Cacheable("latestRescues")
    @Transactional(readOnly = true)
    public List<RescueDtos.RescueResponse> latestPublic(int size) {
        Page<Rescue> result = rescueRepository.findAll(
                publicSpec(null, null, null),
                PageRequest.of(0, Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        boolean loggedIn = SecuritySupport.currentUser().isPresent();
        return result.getContent().stream()
                .map(mapper::toRescueResponse)
                .map(response -> loggedIn ? response : toPublicResponse(response))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueDtos.RescueResponse> listMine(RescueStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<Rescue> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("publisher").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Rescue> result = rescueRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toRescueResponse);
    }

    @Transactional(readOnly = true)
    public RescueDtos.RescueResponse detail(Long id) {
        Rescue rescue = getEntity(id);
        if (!rescue.getStatus().isPublicVisible() && !canAccessPrivate(rescue)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Rescue record is not available");
        }
        RescueDtos.RescueResponse response = mapper.toRescueResponse(rescue);
        return canAccessPrivate(rescue) ? response : toPublicResponse(response);
    }

    @Transactional
    public RescueDtos.RescueResponse create(RescueDtos.SaveRescueRequest request) {
        User publisher = userService.currentUser();
        moderationService.validateText("Rescue content", request.location(), request.animalCondition(), request.description(), request.contact());
        Rescue rescue = new Rescue();
        fillRescue(rescue, request);
        rescue.setStatus(RescueStatus.PENDING_REVIEW);
        rescue.setReviewComment(null);
        rescue.setPublisher(publisher);
        RescueDtos.RescueResponse response = mapper.toRescueResponse(rescueRepository.save(rescue));
        cacheInvalidationService.evictPublicCaches();
        return response;
    }

    @Transactional
    public RescueDtos.RescueResponse update(Long id, RescueDtos.SaveRescueRequest request) {
        Rescue rescue = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(rescue.getPublisher().getId());
        moderationService.validateText("Rescue content", request.location(), request.animalCondition(), request.description(), request.contact());
        fillRescue(rescue, request);
        if (!SecuritySupport.isAdmin()) {
            rescue.setStatus(RescueStatus.PENDING_REVIEW);
            rescue.setReviewComment(null);
        } else if (!rescue.getPublisher().getId().equals(SecuritySupport.requireUser().id())) {
            notificationService.notifyUser(rescue.getPublisher(), NotificationType.AUDIT_RESULT,
                    "ADMIN_EDIT_RESCUE", "管理员编辑了你的救助信息「" + rescue.getLocation() + "」",
                    "RESCUE", rescue.getId());
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toRescueResponse(rescue);
    }

    @Transactional
    public void offline(Long id) {
        Rescue rescue = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(rescue.getPublisher().getId());
        rescue.setStatus(RescueStatus.OFFLINE);
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public RescueDtos.RescueResponse updateStatus(Long id, RescueDtos.UpdateRescueStatusRequest request) {
        Rescue rescue = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(rescue.getPublisher().getId());
        if (request.status() == RescueStatus.PENDING_REVIEW || request.status() == RescueStatus.REJECTED) {
            throw new BusinessException("This rescue status cannot be set through the normal status update endpoint");
        }
        rescue.setStatus(request.status());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toRescueResponse(rescue);
    }

    @Transactional(readOnly = true)
    public Rescue getEntity(Long id) {
        return rescueRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Rescue record does not exist"));
    }

    private void fillRescue(Rescue rescue, RescueDtos.SaveRescueRequest request) {
        rescue.setLocation(request.location());
        rescue.setAnimalCondition(request.animalCondition());
        rescue.setContact(request.contact());
        rescue.setDescription(request.description());
        rescue.getImageUrls().clear();
        if (request.imageUrls() != null) {
            rescue.getImageUrls().addAll(request.imageUrls());
        }
    }

    private boolean canAccessPrivate(Rescue rescue) {
        return SecuritySupport.currentUser().isPresent();
    }

    private RescueDtos.RescueResponse toPublicResponse(RescueDtos.RescueResponse response) {
        return new RescueDtos.RescueResponse(
                response.id(),
                response.location(),
                response.animalCondition(),
                maskContact(response.contact()),
                response.description(),
                response.imageUrls(),
                response.status(),
                response.statusText(),
                response.reviewComment(),
                response.publisherId(),
                response.publisherNickname(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private String maskContact(String contact) {
        if (contact == null || contact.isBlank()) {
            return "";
        }
        if (contact.length() >= 11) {
            return contact.substring(0, 3) + "****" + contact.substring(contact.length() - 4);
        }
        return "Contact hidden";
    }

    private Specification<Rescue> publicSpec(String keyword, String region, RescueStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && status.isPublicVisible()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(List.of(
                        RescueStatus.PENDING_PROCESS,
                        RescueStatus.PROCESSING,
                        RescueStatus.COMPLETED
                )));
            }
            if (region != null && !region.isBlank()) {
                predicates.add(cb.like(root.get("location"), "%" + region.trim() + "%"));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("location"), like),
                        cb.like(root.get("animalCondition"), like),
                        cb.like(root.get("description"), like)
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
