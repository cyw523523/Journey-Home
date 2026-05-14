package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.SystemNotification;
import com.guitu.domain.User;
import com.guitu.domain.enums.NotificationType;
import com.guitu.dto.NotificationDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.SystemNotificationRepository;
import com.guitu.repository.UserRepository;
import com.guitu.security.SecuritySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final SystemNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;

    public NotificationService(SystemNotificationRepository notificationRepository, UserRepository userRepository, DtoMapper mapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public void notifyUser(User recipient, NotificationType type, String title, String content, String relatedTargetType, Long relatedTargetId) {
        if (recipient == null) {
            return;
        }
        SystemNotification notification = new SystemNotification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedTargetType(relatedTargetType);
        notification.setRelatedTargetId(relatedTargetId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyAdmins(NotificationType type, String title, String content, String relatedTargetType, Long relatedTargetId) {
        for (User admin : userRepository.findAll().stream().filter(user -> user.getRole().name().equals("ADMIN")).toList()) {
            notifyUser(admin, type, title, content, relatedTargetType, relatedTargetId);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDtos.NotificationResponse> mine(int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Page<SystemNotification> result = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result, mapper::toNotificationResponse);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.NotificationSummary summary() {
        Long userId = SecuritySupport.requireUser().id();
        return new NotificationDtos.NotificationSummary(notificationRepository.countByRecipientIdAndReadFlagFalse(userId));
    }

    @Transactional
    public void markRead(Long id) {
        SystemNotification notification = getOwnedNotification(id);
        notification.setReadFlag(true);
        notification.setReadAt(LocalDateTime.now());
    }

    @Transactional
    public void markAllRead() {
        Long userId = SecuritySupport.requireUser().id();
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).forEach(notification -> {
            notification.setReadFlag(true);
            notification.setReadAt(LocalDateTime.now());
        });
    }

    private SystemNotification getOwnedNotification(Long id) {
        SystemNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Notification does not exist"));
        if (!notification.getRecipient().getId().equals(SecuritySupport.requireUser().id())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot access this notification");
        }
        return notification;
    }
}
