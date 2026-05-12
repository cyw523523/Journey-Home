package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.AdoptApply;
import com.guitu.domain.Animal;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
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

import java.util.ArrayList;
import java.util.List;

@Service
public class AdoptApplyService {
    private final AdoptApplyRepository adoptApplyRepository;
    private final AnimalService animalService;
    private final UserService userService;
    private final DtoMapper mapper;

    public AdoptApplyService(
            AdoptApplyRepository adoptApplyRepository,
            AnimalService animalService,
            UserService userService,
            DtoMapper mapper
    ) {
        this.adoptApplyRepository = adoptApplyRepository;
        this.animalService = animalService;
        this.userService = userService;
        this.mapper = mapper;
    }

    @Transactional
    public AdoptApplyDtos.ApplyResponse create(AdoptApplyDtos.CreateApplyRequest request) {
        User applicant = userService.currentUser();
        Animal animal = animalService.getEntity(request.animalId());
        if (animal.getStatus() != AnimalStatus.WAITING_ADOPTION) {
            throw new BusinessException("该动物当前不可申请领养");
        }
        boolean duplicated = adoptApplyRepository.existsByAnimalIdAndApplicantIdAndStatus(
                animal.getId(),
                applicant.getId(),
                ApplyStatus.PENDING_REVIEW
        );
        if (duplicated) {
            throw new BusinessException("请勿重复提交申请");
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
        return mapper.toApplyResponse(adoptApplyRepository.save(apply));
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
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权限操作");
        }
        return mapper.toApplyResponse(apply);
    }

    @Transactional
    public void cancel(Long id) {
        AdoptApply apply = getEntity(id);
        if (!SecuritySupport.requireUser().id().equals(apply.getApplicant().getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权限操作");
        }
        if (apply.getStatus() != ApplyStatus.PENDING_REVIEW) {
            throw new BusinessException("仅允许取消待审核申请");
        }
        apply.setStatus(ApplyStatus.CANCELED);
    }

    @Transactional(readOnly = true)
    public AdoptApply getEntity(Long id) {
        return adoptApplyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "领养申请不存在"));
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
