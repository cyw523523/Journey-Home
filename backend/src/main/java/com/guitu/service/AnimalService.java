package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.Animal;
import com.guitu.domain.User;
import com.guitu.domain.enums.AnimalGender;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.AnimalType;
import com.guitu.domain.enums.UserRole;
import com.guitu.dto.AnimalDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AnimalRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AnimalService {
    private static final Set<AnimalStatus> OWNER_ALLOWED_STATUSES = EnumSet.of(
            AnimalStatus.WAITING_RESCUE,
            AnimalStatus.RESCUING,
            AnimalStatus.OFFLINE
    );

    private final AnimalRepository animalRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final ContentModerationService moderationService;
    private final CacheInvalidationService cacheInvalidationService;

    public AnimalService(
            AnimalRepository animalRepository,
            UserService userService,
            DtoMapper mapper,
            ContentModerationService moderationService,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.animalRepository = animalRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.moderationService = moderationService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AnimalDtos.AnimalResponse> listPublic(
            String keyword,
            AnimalType type,
            AnimalGender gender,
            AnimalStatus status,
            String region,
            int page,
            int size
    ) {
        Page<Animal> result = animalRepository.findAll(
                publicSpec(keyword, type, gender, status, region),
                pageRequest(page, size)
        );
        return PageResponse.from(result, mapper::toAnimalResponse);
    }

    @Cacheable("latestAnimals")
    @Transactional(readOnly = true)
    public List<AnimalDtos.AnimalResponse> latestPublic(int size) {
        Page<Animal> result = animalRepository.findAll(
                publicSpec(null, null, null, null, null),
                PageRequest.of(0, Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return result.getContent().stream().map(mapper::toAnimalResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AnimalDtos.AnimalResponse> listMine(AnimalStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<Animal> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("publisher").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Animal> result = animalRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toAnimalResponse);
    }

    @Transactional(readOnly = true)
    public AnimalDtos.AnimalResponse detail(Long id) {
        Animal animal = getEntity(id);
        if (!animal.getStatus().isPublicVisible() && !canAccessPrivate(animal)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "该动物信息不存在或暂不可访问");
        }
        return mapper.toAnimalResponse(animal);
    }

    @Transactional
    public AnimalDtos.AnimalResponse create(AnimalDtos.SaveAnimalRequest request) {
        User publisher = userService.currentUser();
        moderationService.validateText("Animal description", request.foundRegion(), request.healthCondition(), request.description());
        Animal animal = new Animal();
        fillAnimal(animal, request);
        animal.setStatus(AnimalStatus.PENDING_REVIEW);
        animal.setReviewComment(null);
        animal.setPublisher(publisher);
        AnimalDtos.AnimalResponse response = mapper.toAnimalResponse(animalRepository.save(animal));
        cacheInvalidationService.evictPublicCaches();
        return response;
    }

    @Transactional
    public AnimalDtos.AnimalResponse update(Long id, AnimalDtos.SaveAnimalRequest request) {
        Animal animal = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(animal.getPublisher().getId());
        moderationService.validateText("Animal description", request.foundRegion(), request.healthCondition(), request.description());
        fillAnimal(animal, request);
        if (!SecuritySupport.isAdmin()) {
            animal.setStatus(AnimalStatus.PENDING_REVIEW);
            animal.setReviewComment(null);
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toAnimalResponse(animal);
    }

    @Transactional
    public void offline(Long id) {
        Animal animal = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(animal.getPublisher().getId());
        animal.setStatus(AnimalStatus.OFFLINE);
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public AnimalDtos.AnimalResponse updateStatus(Long id, AnimalDtos.UpdateAnimalStatusRequest request) {
        Animal animal = getEntity(id);
        SecuritySupport.requireOwnerOrAdmin(animal.getPublisher().getId());
        if (!SecuritySupport.isAdmin() && !OWNER_ALLOWED_STATUSES.contains(request.status())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot set the animal to this status");
        }
        animal.setStatus(request.status());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toAnimalResponse(animal);
    }

    @Transactional(readOnly = true)
    public Animal getEntity(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "动物档案不存在或已下架"));
    }

    private void fillAnimal(Animal animal, AnimalDtos.SaveAnimalRequest request) {
        animal.setType(request.type());
        animal.setGender(request.gender());
        animal.setAge(request.age());
        animal.setFoundRegion(request.foundRegion());
        animal.setHealthCondition(request.healthCondition());
        animal.setDescription(request.description());
        animal.getImageUrls().clear();
        animal.getImageUrls().addAll(request.imageUrls());
        animal.setCoverImageUrl(request.imageUrls().isEmpty() ? null : request.imageUrls().get(0));
    }

    private boolean canAccessPrivate(Animal animal) {
        return SecuritySupport.currentUser()
                .map(principal -> principal.role() == UserRole.ADMIN || principal.id().equals(animal.getPublisher().getId()))
                .orElse(false);
    }

    private Specification<Animal> publicSpec(String keyword, AnimalType type, AnimalGender gender, AnimalStatus status, String region) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && status.isPublicVisible()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(List.of(
                        AnimalStatus.WAITING_RESCUE,
                        AnimalStatus.RESCUING,
                        AnimalStatus.WAITING_ADOPTION,
                        AnimalStatus.ADOPTED
                )));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (region != null && !region.isBlank()) {
                predicates.add(cb.like(root.get("foundRegion"), "%" + region.trim() + "%"));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("foundRegion"), like),
                        cb.like(root.get("healthCondition"), like),
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
