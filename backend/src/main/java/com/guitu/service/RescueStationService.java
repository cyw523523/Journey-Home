package com.guitu.service;

import com.guitu.common.PageResponse;
import com.guitu.domain.RescueStation;
import com.guitu.domain.User;
import com.guitu.domain.UserFollow;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.domain.enums.NotificationType;
import com.guitu.dto.RescueStationDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.RescueStationRepository;
import com.guitu.repository.UserFollowRepository;
import com.guitu.security.SecuritySupport;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RescueStationService {
    private final RescueStationRepository stationRepository;
    private final UserFollowRepository followRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;
    private final CacheInvalidationService cacheInvalidationService;
    private final EntityManager entityManager;

    public RescueStationService(
            RescueStationRepository stationRepository,
            UserFollowRepository followRepository,
            UserService userService,
            DtoMapper mapper,
            NotificationService notificationService,
            CacheInvalidationService cacheInvalidationService,
            EntityManager entityManager
    ) {
        this.stationRepository = stationRepository;
        this.followRepository = followRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.entityManager = entityManager;
    }

    @Transactional
    public RescueStationDtos.StationResponse apply(RescueStationDtos.ApplyRequest request) {
        User user = userService.currentUser();
        if (stationRepository.existsByUserId(user.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "你已经提交过救助站认证申请");
        }
        if (user.getRole() != com.guitu.domain.enums.UserRole.RESCUER) {
            user.setRole(com.guitu.domain.enums.UserRole.RESCUER);
        }
        RescueStation station = new RescueStation();
        station.setUser(user);
        station.setStationName(request.stationName());
        station.setDescription(request.description());
        station.setAddress(request.address());
        station.setContactPhone(request.contactPhone());
        station.setImageUrl(request.imageUrl());
        station.setCertificationStatus(CertificationStatus.PENDING);
        station.setFollowerCount(0);
        cacheInvalidationService.evictPublicCaches();
        return mapper.toStationResponse(stationRepository.save(station));
    }

    @Transactional(readOnly = true)
    public RescueStationDtos.StationResponse getMyStation() {
        Long userId = SecuritySupport.requireUser().id();
        return getStationByUserId(userId);
    }

    @Transactional(readOnly = true)
    public RescueStationDtos.StationResponse getPublicStation(Long userId) {
        return getStationByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueStationDtos.StationResponse> listAll(CertificationStatus status, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Page<RescueStation> stations;
        if (status != null) {
            Specification<RescueStation> spec = (root, query, cb) -> cb.equal(root.get("certificationStatus"), status);
            stations = stationRepository.findAll(spec, pageable);
        } else {
            stations = stationRepository.findAll(pageable);
        }
        return PageResponse.from(stations, mapper::toStationResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueStationDtos.StationResponse> listApproved(int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Specification<RescueStation> spec = (root, query, cb) -> cb.equal(root.get("certificationStatus"), CertificationStatus.APPROVED);
        Page<RescueStation> stations = stationRepository.findAll(spec, pageable);
        return PageResponse.from(stations, mapper::toStationResponse);
    }

    @Transactional
    public RescueStationDtos.StationResponse updateProfile(RescueStationDtos.UpdateProfileRequest request) {
        Long userId = SecuritySupport.requireUser().id();
        RescueStation station = stationRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "你还没有创建救助站档案"));
        station.setStationName(request.stationName());
        station.setDescription(request.description());
        station.setAddress(request.address());
        station.setContactPhone(request.contactPhone());
        station.setImageUrl(request.imageUrl());
        cacheInvalidationService.evictPublicCaches();
        return mapper.toStationResponse(stationRepository.save(station));
    }

    @Transactional
    public void follow(Long stationUserId) {
        Long myId = SecuritySupport.requireUser().id();
        if (myId.equals(stationUserId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能关注自己");
        }
        if (!stationRepository.existsByUserId(stationUserId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "该用户没有救助站档案");
        }
        java.util.Optional<UserFollow> existing = followRepository.findByFollowerAndStation(myId, stationUserId);
        if (existing.isPresent()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "你已经关注了这个救助站");
        }
        UserFollow follow = new UserFollow();
        User follower = userService.currentUser();
        User stationUser = userService.getById(stationUserId);
        follow.setFollower(follower);
        follow.setStationUser(stationUser);
        followRepository.save(follow);
        RescueStation station = stationRepository.findByUserId(stationUserId).orElse(null);
        if (station != null) {
            station.setFollowerCount(station.getFollowerCount() + 1);
            stationRepository.save(station);
        }
        notificationService.notifyUser(
                stationUser, NotificationType.SYSTEM,
                "NEW_FOLLOWER", follower.getNickname() + " 关注了你的救助站",
                "RESCUE_STATION", station != null ? station.getId() : null
        );
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional
    public void unfollow(Long stationUserId) {
        Long myId = SecuritySupport.requireUser().id();
        java.util.Optional<UserFollow> existing = followRepository.findByFollowerAndStation(myId, stationUserId);
        UserFollow follow = existing.orElse(null);
        if (follow == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "你还没有关注这个救助站");
        }
        followRepository.delete(follow);
        RescueStation station = stationRepository.findByUserId(stationUserId).orElse(null);
        if (station != null && station.getFollowerCount() > 0) {
            station.setFollowerCount(Math.max(0, station.getFollowerCount() - 1));
            stationRepository.save(station);
        }
        cacheInvalidationService.evictPublicCaches();
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueStationDtos.FollowResponse> listMyFollowers(int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Pageable pageable = pageRequest(page, size);
        Page<UserFollow> follows = followRepository.findByStationUserId(userId, pageable);
        return PageResponse.from(follows, mapper::toFollowResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<RescueStationDtos.FollowResponse> listFollowingStations(int page, int size) {
        Long userId = SecuritySupport.requireUser().id();
        Pageable pageable = pageRequest(page, size);
        Page<UserFollow> follows = followRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(follows, mapper::toFollowResponse);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long stationUserId) {
        try {
            Long myId = SecuritySupport.requireUser().id();
            return followRepository.findByFollowerAndStation(myId, stationUserId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public RescueStationDtos.DashboardResponse dashboard(Long userId) {
        RescueStation station = stationRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "救助站不存在"));
        long rescueCount = safeCount("SELECT COUNT(r) FROM com.guitu.domain.Rescue r WHERE r.publisher.id = :pid", userId);
        long animalCount = safeCount("SELECT COUNT(a) FROM com.guitu.domain.Animal a WHERE a.publisher.id = :pid", userId);
        long donationDemandCount = safeCount("SELECT COUNT(d) FROM com.guitu.domain.SupplyDemand d WHERE d.publisher.id = :pid", userId);
        long totalDonationRecords = safeCount("SELECT COUNT(r) FROM com.guitu.domain.DonationRecord r WHERE r.demand.publisher.id = :pid", userId);
        long volunteerTaskCount = safeCount("SELECT COUNT(t) FROM com.guitu.domain.VolunteerTask t WHERE t.publisher.id = :pid", userId);
        long totalVolunteerApplications = safeCount("SELECT COUNT(a) FROM com.guitu.domain.VolunteerApplication a WHERE a.task.publisher.id = :pid", userId);
        return new RescueStationDtos.DashboardResponse(
                station.getId(),
                station.getStationName(),
                (int) rescueCount,
                (int) animalCount,
                (int) donationDemandCount,
                (int) totalDonationRecords,
                (int) volunteerTaskCount,
                (int) totalVolunteerApplications,
                station.getFollowerCount()
        );
    }

    @Transactional
    public RescueStationDtos.StationResponse adminCertify(Long userId, CertificationStatus status, String reason) {
        RescueStation station = stationRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "救助站不存在"));
        station.setCertificationStatus(status);
        station.setRejectReason(status == CertificationStatus.REJECTED ? reason : null);
        notificationService.notifyUser(
                station.getUser(),
                NotificationType.ACCOUNT_ACTION,
                status == CertificationStatus.APPROVED ? "恭喜！你的救助站已通过认证" : "很遗憾，你的救助站认证未通过",
                reason,
                "RESCUE_STATION", station.getId()
        );
        cacheInvalidationService.evictPublicCaches();
        return mapper.toStationResponse(stationRepository.save(station));
    }

    private RescueStationDtos.StationResponse getStationByUserId(Long userId) {
        RescueStation station = stationRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "该用户的救助站档案不存在"));
        return mapper.toStationResponse(station);
    }

    private long safeCount(String jpql, Long pid) {
        try {
            return ((Number) entityManager
                    .createQuery(jpql)
                    .setParameter("pid", pid)
                    .getSingleResult()).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
