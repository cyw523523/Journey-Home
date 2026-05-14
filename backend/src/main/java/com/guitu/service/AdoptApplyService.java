package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.dto.AdoptApplyDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AdoptApplyRepository;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdoptApplyService {
    private final AdoptApplyRepository adoptApplyRepository;
    private final AnimalService animalService;
    private final UserService userService;
    private final DtoMapper mapper;
    private final AntiAbuseService antiAbuseService;
    private final ContentModerationService moderationService;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;

    public AdoptApplyService(
            AdoptApplyRepository adoptApplyRepository,
            AnimalService animalService,
            UserService userService,
            DtoMapper mapper,
            AntiAbuseService antiAbuseService,
            ContentModerationService moderationService,
            NotificationService notificationService,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.adoptApplyRepository = adoptApplyRepository;
        this.animalService = animalService;
        this.userService = userService;
        this.mapper = mapper;
        this.antiAbuseService = antiAbuseService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public AdoptApplyDtos.ApplyResponse create(AdoptApplyDtos.CreateApplyRequest request) {
        User applicant = userService.currentUser();
        antiAbuseService.check("adoption-apply", applicant.getId().toString(), 4, Duration.ofHours(6), "Too many adoption applications, please try again later");
        moderationService.validateText("Application content", request.applicantName(), request.contact(), request.reason(), request.livingCondition(), request.experience());

        Animal animal = animalService.getEntity(request.animalId());
        if (animal.getStatus() != AnimalStatus.WAITING_ADOPTION) {
            throw new BusinessException("This animal is not open for adoption right now");
        }
        boolean duplicated = adoptApplyRepository.existsByAnimalIdAndApplicantIdAndStatus(
                animal.getId(),
                applicant.getId(),
                ApplyStatus.PENDING_REVIEW
        );
        if (duplicated) {
            throw new BusinessException("Please do not submit duplicate applications");
        }

        AdoptApply apply = new AdoptApply();
        apply.setAnimal(animal);
        apply.setApplicant(applicant);
        apply.setApplicantName(request.applicantName());
        apply.setContact(request.contact());
        apply.setReason(request.reason());
        apply.setLivingCondition(request.livingCondition());
        apply.setExperience(request.experience());
        apply.setStatus(ApplyStatus.PENDING_REVIEW);
        AdoptApply saved = adoptApplyRepository.save(apply);
        notificationService.notifyUser(applicant, NotificationType.AUDIT_RESULT, "Adoption application submitted", "Your application has been submitted and is waiting for admin review.", "ADOPT_APPLY", saved.getId());
        notificationService.notifyAdmins(NotificationType.AUDIT_RESULT, "New adoption application pending review", "A new adoption application is waiting for review.", "ADOPT_APPLY", saved.getId());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toApplyResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdoptApplyDtos.ApplyResponse> listMine(ApplyStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Page<AdoptApply> result = adoptApplyRepository.findAll(applySpec(userId, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toApplyResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdoptApplyDtos.ApplyResponse> adminList(ApplyStatus status, int page, int size) {
        Page<AdoptApply> result = adoptApplyRepository.findAll(applySpec(null, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toApplyResponse);
    }

    @Transactional(readOnly = true)
    public AdoptApplyDtos.ApplyResponse detail(Long id) {
        AdoptApply apply = getEntity(id);
        if (!SecuritySupport.requireUser().id().equals(apply.getApplicant().getId()) && !SecuritySupport.isAdmin()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot access this application");
        }
        return mapper.toApplyResponse(apply);
    }

    @Transactional
    public void cancel(Long id) {
        AdoptApply apply = getEntity(id);
        if (!SecuritySupport.requireUser().id().equals(apply.getApplicant().getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot cancel this application");
        }
        if (apply.getStatus() != ApplyStatus.PENDING_REVIEW) {
            throw new BusinessException("Only pending applications can be canceled");
        }
        apply.setStatus(ApplyStatus.CANCELED);
        notificationService.notifyUser(apply.getApplicant(), NotificationType.AUDIT_RESULT, "Adoption application canceled", "Your application has been canceled.", "ADOPT_APPLY", apply.getId());
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional(readOnly = true)
    public AdoptApply getEntity(Long id) {
        return adoptApplyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Adoption application does not exist"));
    }

    private Specification<AdoptApply> applySpec(Long applicantId, ApplyStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (applicantId != null) {
                predicates.add(cb.equal(root.get("applicant").get("id"), applicantId));
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
