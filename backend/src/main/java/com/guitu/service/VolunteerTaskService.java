package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.Rescue;
import com.guitu.domain.User;
import com.guitu.domain.VolunteerApplication;
import com.guitu.domain.VolunteerTask;
import com.guitu.domain.enums.ApplicationStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.VolunteerTaskStatus;
import com.guitu.dto.VolunteerTaskDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.VolunteerApplicationRepository;
import com.guitu.repository.VolunteerTaskRepository;
import com.guitu.security.SecuritySupport;
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
public class VolunteerTaskService {
    private final VolunteerTaskRepository taskRepository;
    private final VolunteerApplicationRepository applicationRepository;
    private final UserService userService;
    private final RescueService rescueService;
    private final DtoMapper mapper;
    private final CacheInvalidationService cacheInvalidationService;
    private final NotificationService notificationService;
    private final ContentModerationService moderationService;

    public VolunteerTaskService(
            VolunteerTaskRepository taskRepository,
            VolunteerApplicationRepository applicationRepository,
            UserService userService,
            RescueService rescueService,
            DtoMapper mapper,
            CacheInvalidationService cacheInvalidationService,
            NotificationService notificationService,
            ContentModerationService moderationService
    ) {
        this.taskRepository = taskRepository;
        this.applicationRepository = applicationRepository;
        this.userService = userService;
        this.rescueService = rescueService;
        this.mapper = mapper;
        this.cacheInvalidationService = cacheInvalidationService;
        this.notificationService = notificationService;
        this.moderationService = moderationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<VolunteerTaskDtos.VolunteerTaskResponse> listPublic(String keyword, String region, VolunteerTaskStatus status, int page, int size) {
        Page<VolunteerTask> result = taskRepository.findAll(publicSpec(keyword, region, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toVolunteerTaskResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<VolunteerTaskDtos.VolunteerTaskResponse> listMine(VolunteerTaskStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<VolunteerTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("publisher").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<VolunteerTask> result = taskRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toVolunteerTaskResponse);
    }

    @Transactional(readOnly = true)
    public VolunteerTaskDtos.VolunteerTaskResponse detail(Long id) {
        VolunteerTask task = getEntity(id);
        if (!task.getStatus().isPublicVisible() && !canAccessPrivate(task)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "任务不存在或不可查看");
        }
        return mapper.toVolunteerTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<VolunteerTaskDtos.ApplicationResponse> listApplications(Long taskId, int page, int size) {
        VolunteerTask task = getEntity(taskId);
        SecuritySupport.requireOwnerOrAdmin(task.getPublisher().getId());
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VolunteerApplication> apps = applicationRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable);
        return apps.getContent().stream().map(mapper::toApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<VolunteerTaskDtos.ApplicationResponse> listMyApplications(ApplicationStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<VolunteerApplication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("volunteer").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<VolunteerApplication> result = applicationRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toApplicationResponse);
    }

    @Transactional
    public VolunteerTaskDtos.VolunteerTaskResponse create(VolunteerTaskDtos.SaveVolunteerTaskRequest request) {
        User publisher = userService.currentUser();
        moderationService.validateText("Volunteer task content", request.title(), request.description(), request.location());
        VolunteerTask task = new VolunteerTask();
        fillTask(task, request);
        task.setCurrentVolunteers(0);
        task.setStatus(VolunteerTaskStatus.PENDING_REVIEW);
        task.setReviewComment(null);
        task.setPublisher(publisher);
        if (request.relatedRescueId() != null) {
            Rescue rescue = rescueService.getEntity(request.relatedRescueId());
            task.setRelatedRescue(rescue);
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toVolunteerTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public VolunteerTaskDtos.VolunteerTaskResponse update(Long id, VolunteerTaskDtos.SaveVolunteerTaskRequest request) {
        VolunteerTask task = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(task.getPublisher().getId());
        moderationService.validateText("Volunteer task content", request.title(), request.description(), request.location());
        fillTask(task, request);
        if (!SecuritySupport.isAdmin()) {
            task.setStatus(VolunteerTaskStatus.PENDING_REVIEW);
            task.setReviewComment(null);
        }
        if (request.relatedRescueId() != null) {
            Rescue rescue = rescueService.getEntity(request.relatedRescueId());
            task.setRelatedRescue(rescue);
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toVolunteerTaskResponse(task);
    }

    @Transactional
    public void offline(Long id) {
        VolunteerTask task = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(task.getPublisher().getId());
        task.setStatus(VolunteerTaskStatus.CANCELLED);
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public VolunteerTaskDtos.VolunteerTaskResponse updateStatus(Long id, VolunteerTaskDtos.UpdateTaskStatusRequest request) {
        VolunteerTask task = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(task.getPublisher().getId());
        if (request.status() == VolunteerTaskStatus.REJECTED) {
            throw new BusinessException("This task status cannot be set through the normal status update endpoint");
        }
        task.setStatus(request.status());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toVolunteerTaskResponse(task);
    }

    @Transactional
    public VolunteerTaskDtos.ApplicationResponse apply(Long taskId, VolunteerTaskDtos.ApplyRequest request) {
        User volunteer = userService.currentUser();
        VolunteerTask task = getEntity(taskId);
        if (task.getStatus() != VolunteerTaskStatus.RECRUITING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前任务不在招募中");
        }
        if (task.getCurrentVolunteers() >= task.getMaxVolunteers()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "志愿者人数已满");
        }
        boolean alreadyApplied = applicationRepository.countByVolunteerIdAndTaskIdAndStatus(volunteer.getId(), taskId, ApplicationStatus.PENDING) > 0;
        if (alreadyApplied) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "你已经申请过该任务");
        }
        VolunteerApplication application = new VolunteerApplication();
        application.setTask(task);
        application.setVolunteer(volunteer);
        application.setMessage(request.message());
        application.setStatus(ApplicationStatus.PENDING);
        applicationRepository.save(application);
        notificationService.notifyUser(task.getPublisher(), NotificationType.SYSTEM,
                "NEW_VOLUNTEER_APPLY", volunteer.getNickname() + " 报名参加了你的志愿者任务「" + task.getTitle() + "」",
                "VOLUNTEER_TASK", task.getId());
        return mapper.toApplicationResponse(application);
    }

    @Transactional
    public VolunteerTaskDtos.ApplicationResponse reviewApplication(Long applicationId, VolunteerTaskDtos.ReviewApplicationRequest request) {
        VolunteerApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "申请记录不存在"));
        SecuritySupport.requireOwnerOrAdmin(app.getTask().getPublisher().getId());
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该申请已处理");
        }
        app.setStatus(request.status());
        app.setReviewComment(request.reviewComment());
        if (request.status() == ApplicationStatus.APPROVED) {
            VolunteerTask task = app.getTask();
            task.setCurrentVolunteers(task.getCurrentVolunteers() + 1);
            if (task.getCurrentVolunteers() >= task.getMaxVolunteers()) {
                task.setStatus(VolunteerTaskStatus.IN_PROGRESS);
            }
            notificationService.notifyUser(app.getVolunteer(), NotificationType.SYSTEM,
                    "VOLUNTEER_APPROVED", "你报名的志愿者任务「" + task.getTitle() + "」已通过审核",
                    "VOLUNTEER_TASK", task.getId());
        } else if (request.status() == ApplicationStatus.REJECTED) {
            notificationService.notifyUser(app.getVolunteer(), NotificationType.SYSTEM,
                    "VOLUNTEER_REJECTED", "你报名的志愿者任务「" + app.getTask().getTitle() + "」未通过审核",
                    "VOLUNTEER_APPLICATION", app.getId());
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toApplicationResponse(app);
    }

    @Transactional
    public VolunteerTaskDtos.ApplicationResponse completeApplication(Long applicationId) {
        VolunteerApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "申请记录不存在"));
        SecuritySupport.requireOwnerOrAdmin(app.getTask().getPublisher().getId());
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只能确认已通过的志愿者完成任务");
        }
        app.setStatus(ApplicationStatus.COMPLETED);
        app.setCompletedAt(LocalDateTime.now());
        VolunteerTask task = app.getTask();
        long completedCount = applicationRepository.countByTaskIdAndStatus(task.getId(), ApplicationStatus.COMPLETED);
        if (completedCount >= task.getMaxVolunteers()) {
            task.setStatus(VolunteerTaskStatus.COMPLETED);
        }
        notificationService.notifyUser(app.getVolunteer(), NotificationType.SYSTEM,
                "VOLUNTEER_COMPLETED", "你在志愿者任务「" + task.getTitle() + "」中的工作已确认为完成",
                "VOLUNTEER_APPLICATION", app.getId());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toApplicationResponse(app);
    }

    @Transactional(readOnly = true)
    public VolunteerTask getEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "志愿者任务不存在"));
    }

    private void fillTask(VolunteerTask task, VolunteerTaskDtos.SaveVolunteerTaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setLocation(request.location());
        task.setMaxVolunteers(request.maxVolunteers());
        task.setScheduledTime(request.scheduledTime());
        task.setImageUrl(request.imageUrl());
    }

    private boolean canAccessPrivate(VolunteerTask task) {
        return SecuritySupport.currentUser().isPresent();
    }

    private Specification<VolunteerTask> publicSpec(String keyword, String region, VolunteerTaskStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && status.isPublicVisible()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(List.of(
                        VolunteerTaskStatus.RECRUITING, VolunteerTaskStatus.IN_PROGRESS, VolunteerTaskStatus.COMPLETED
                )));
            }
            if (region != null && !region.isBlank()) {
                predicates.add(cb.like(root.get("location"), "%" + region.trim() + "%"));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("description"), like),
                        cb.like(root.get("location"), like)
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
