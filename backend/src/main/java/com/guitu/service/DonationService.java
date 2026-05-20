package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.DonationRecord;
import com.guitu.domain.SupplyDemand;
import com.guitu.domain.User;
import com.guitu.domain.enums.DonationStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.domain.enums.SupplyCategory;
import com.guitu.dto.DonationDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.DonationRecordRepository;
import com.guitu.repository.SupplyDemandRepository;
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
public class DonationService {
    private final SupplyDemandRepository demandRepository;
    private final DonationRecordRepository recordRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final CacheInvalidationService cacheInvalidationService;
    private final NotificationService notificationService;
    private final ContentModerationService moderationService;

    public DonationService(
            SupplyDemandRepository demandRepository,
            DonationRecordRepository recordRepository,
            UserService userService,
            DtoMapper mapper,
            CacheInvalidationService cacheInvalidationService,
            NotificationService notificationService,
            ContentModerationService moderationService
    ) {
        this.demandRepository = demandRepository;
        this.recordRepository = recordRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.cacheInvalidationService = cacheInvalidationService;
        this.notificationService = notificationService;
        this.moderationService = moderationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationDtos.SupplyDemandResponse> listPublic(String keyword, SupplyCategory category, DonationStatus status, int page, int size) {
        Page<SupplyDemand> result = demandRepository.findAll(publicSpec(keyword, category, status), pageRequest(page, size));
        return PageResponse.from(result, mapper::toSupplyDemandResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationDtos.SupplyDemandResponse> listMine(DonationStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<SupplyDemand> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("publisher").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<SupplyDemand> result = demandRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toSupplyDemandResponse);
    }

    @Transactional(readOnly = true)
    public DonationDtos.SupplyDemandResponse detail(Long id) {
        SupplyDemand demand = getDemandEntity(id);
        return mapper.toSupplyDemandResponse(demand);
    }

    @Transactional(readOnly = true)
    public List<DonationDtos.DonationRecordResponse> listRecordsByDemand(Long demandId, int page, int size) {
        getDemandEntity(demandId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DonationRecord> records = recordRepository.findByDemandIdOrderByCreatedAtDesc(demandId, pageable);
        return records.getContent().stream().map(mapper::toDonationRecordResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationDtos.DonationRecordResponse> listMyDonations(DonationStatus status, int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Specification<DonationRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("donor").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<DonationRecord> result = recordRepository.findAll(spec, pageRequest(page, size));
        return PageResponse.from(result, mapper::toDonationRecordResponse);
    }

    @Transactional
    public DonationDtos.SupplyDemandResponse create(DonationDtos.SaveSupplyDemandRequest request) {
        User publisher = userService.currentUser();
        moderationService.validateText("Supply demand content", request.title(), request.description());
        SupplyDemand demand = new SupplyDemand();
        fillDemand(demand, request);
        demand.setCurrentQuantity(0);
        demand.setStatus(DonationStatus.PENDING);
        demand.setPublisher(publisher);
        cacheInvalidationService.evictPublicCaches();
        return mapper.toSupplyDemandResponse(demandRepository.save(demand));
    }

    @Transactional
    public DonationDtos.SupplyDemandResponse update(Long id, DonationDtos.SaveSupplyDemandRequest request) {
        SupplyDemand demand = getDemandEntity(id);
        SecuritySupport.requireOwnerOrAdmin(demand.getPublisher().getId());
        moderationService.validateText("Supply demand content", request.title(), request.description());
        fillDemand(demand, request);
        if (!SecuritySupport.isAdmin()) {
            demand.setStatus(DonationStatus.PENDING);
        }
        cacheInvalidationService.evictPublicCaches();
        return mapper.toSupplyDemandResponse(demand);
    }

    @Transactional
    public void offline(Long id) {
        SupplyDemand demand = getDemandEntity(id);
        SecuritySupport.requireOwnerOrAdmin(demand.getPublisher().getId());
        demand.setStatus(DonationStatus.CANCELLED);
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public DonationDtos.DonationRecordResponse donate(Long demandId, DonationDtos.DonateRequest request) {
        SupplyDemand demand = getDemandEntity(demandId);
        if (demand.getStatus() == DonationStatus.COMPLETED || demand.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该物资需求已关闭，无法捐赠");
        }
        if (demand.getCurrentQuantity() + request.quantity() > demand.getTargetQuantity()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "捐赠数量超过剩余需求量");
        }
        User donor = userService.currentUser();
        DonationRecord record = new DonationRecord();
        record.setDemand(demand);
        record.setDonor(donor);
        record.setQuantity(request.quantity());
        record.setDeliveryMethod(request.deliveryMethod());
        record.setTrackingNumber(request.trackingNumber());
        record.setMessage(request.message());
        record.setDonorDisplayName(request.donorDisplayName() != null ? request.donorDisplayName() : donor.getNickname());
        record.setStatus(DonationStatus.CLAIMED);
        demand.setCurrentQuantity(demand.getCurrentQuantity() + request.quantity());
        if (demand.getCurrentQuantity() >= demand.getTargetQuantity()) {
            demand.setStatus(DonationStatus.COMPLETED);
        } else {
            demand.setStatus(DonationStatus.CLAIMED);
        }
        recordRepository.save(record);
        notificationService.notifyUser(demand.getPublisher(), NotificationType.SYSTEM,
                "NEW_DONATION", "有人向你的物资需求「" + demand.getTitle() + "」捐赠了" + request.quantity() + "件物品",
                "SUPPLY_DEMAND", demand.getId());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toDonationRecordResponse(record);
    }

    @Transactional
    public DonationDtos.DonationRecordResponse completeDonation(Long recordId) {
        DonationRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "捐赠记录不存在"));
        SecuritySupport.requireOwnerOrAdmin(record.getDemand().getPublisher().getId());
        if (record.getStatus() == DonationStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该捐赠已确认完成");
        }
        record.setStatus(DonationStatus.COMPLETED);
        record.setCompletedAt(LocalDateTime.now());
        notificationService.notifyUser(record.getDonor(), NotificationType.SYSTEM,
                "DONATION_COMPLETED", "你向「" + record.getDemand().getTitle() + "」的捐赠已被确认收到",
                "DONATION_RECORD", record.getId());
        return mapper.toDonationRecordResponse(record);
    }

    @Transactional(readOnly = true)
    public SupplyDemand getDemandEntity(Long id) {
        return demandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物资需求不存在"));
    }

    private void fillDemand(SupplyDemand demand, DonationDtos.SaveSupplyDemandRequest request) {
        demand.setTitle(request.title());
        demand.setCategory(request.category());
        demand.setTargetQuantity(request.targetQuantity());
        demand.setDescription(request.description());
        demand.setContactName(request.contactName());
        demand.setContactPhone(request.contactPhone());
        demand.setShippingAddress(request.shippingAddress());
        demand.setImageUrl(request.imageUrl());
    }

    private Specification<SupplyDemand> publicSpec(String keyword, SupplyCategory category, DonationStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && status.isPublicVisible()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(List.of(
                        DonationStatus.PENDING, DonationStatus.CLAIMED, DonationStatus.IN_TRANSIT, DonationStatus.COMPLETED
                )));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
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
