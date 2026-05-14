package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.UserRole;
import com.guitu.domain.enums.UserStatus;
import com.guitu.dto.UserDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.UserRepository;
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
public class AdminUserService {
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;

    public AdminUserService(UserRepository userRepository, DtoMapper mapper, NotificationService notificationService, CacheInvalidationService cacheInvalidationService) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.UserProfile> list(String keyword, UserRole role, UserStatus status, int page, int size) {
        Page<User> result = userRepository.findAll(spec(keyword, role, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toUserProfile);
    }

    @Transactional
    public UserDtos.UserProfile update(Long id, UserDtos.AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User does not exist"));
        user.setRole(request.role());
        user.setStatus(request.status());
        notificationService.notifyUser(
                user,
                NotificationType.ACCOUNT_ACTION,
                "Account status updated",
                "Your account role or status has been updated by an administrator.",
                "USER",
                user.getId()
        );
        cacheInvalidationService.evictPublicCaches();
        return mapper.toUserProfile(user);
    }

    private Specification<User> spec(String keyword, UserRole role, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("account"), like),
                        cb.like(root.get("nickname"), like),
                        cb.like(root.get("phone"), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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
