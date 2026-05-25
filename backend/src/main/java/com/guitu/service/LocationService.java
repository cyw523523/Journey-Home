package com.guitu.service;

import com.guitu.domain.Animal;
import com.guitu.domain.RescueStation;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.dto.LocationDtos;
import com.guitu.exception.BusinessException;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.RescueStationRepository;
import com.guitu.security.SecuritySupport;
import com.guitu.util.HaversineUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 地理位置后端业务服务。
 */
@Service
public class LocationService {
    /** 用户端地图中允许展示的动物状态。 */
    private static final Set<AnimalStatus> VISIBLE_ANIMAL_STATUSES = Set.of(
            AnimalStatus.WAITING_RESCUE,
            AnimalStatus.RESCUING,
            AnimalStatus.WAITING_ADOPTION,
            AnimalStatus.ADOPTED
    );

    /** 本功能允许的距离筛选范围，单位：公里。 */
    private static final Set<Double> ALLOWED_DISTANCE_KM = Set.of(1.0, 3.0, 5.0, 10.0);

    private final AnimalRepository animalRepository;
    private final RescueStationRepository rescueStationRepository;
    private final CacheInvalidationService cacheInvalidationService;

    public LocationService(
            AnimalRepository animalRepository,
            RescueStationRepository rescueStationRepository,
            CacheInvalidationService cacheInvalidationService
    ) {
        this.animalRepository = animalRepository;
        this.rescueStationRepository = rescueStationRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    /**
     * 更新动物发现地点经纬度。
     * 普通登录用户只能更新自己发布的动物档案，管理员可以更新全部动物档案。
     */
    @Transactional
    public LocationDtos.AnimalLocationResponse updateAnimalLocation(LocationDtos.AnimalLocationRequest request) {
        SecuritySupport.requireUser();
        validateLatitude(request.latitude());
        validateLongitude(request.longitude());

        Animal animal = animalRepository.findById(request.animalId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "动物档案不存在"));
        SecuritySupport.requireOwnerOrAdmin(animal.getPublisher().getId());

        animal.setFoundLatitude(request.latitude());
        animal.setFoundLongitude(request.longitude());
        Animal saved = animalRepository.save(animal);
        cacheInvalidationService.evictPublicCaches();

        return new LocationDtos.AnimalLocationResponse(
                saved.getId(),
                saved.getFoundLatitude(),
                saved.getFoundLongitude(),
                saved.getUpdatedAt()
        );
    }

    /**
     * 管理员更新救助站位置。
     */
    @Transactional
    public LocationDtos.RescueStationLocationResponse updateRescueStationLocation(LocationDtos.RescueStationLocationRequest request) {
        SecuritySupport.requireAdmin();
        validateLatitude(request.latitude());
        validateLongitude(request.longitude());

        RescueStation station = rescueStationRepository.findById(request.stationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "救助站不存在"));

        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        if (request.addressDetail() != null && !request.addressDetail().isBlank()) {
            station.setAddress(request.addressDetail().trim());
        }
        RescueStation saved = rescueStationRepository.save(station);
        cacheInvalidationService.evictPublicCaches();

        return toStationLocationResponse(saved);
    }

    /**
     * 按距离筛选动物信息。
     */
    @Transactional(readOnly = true)
    public LocationDtos.NearbyAnimalListResponse nearbyAnimals(Double latitude, Double longitude, Double distance) {
        SecuritySupport.requireUser();
        double safeDistance = validateNearbyParams(latitude, longitude, distance);

        List<LocationDtos.NearbyAnimalResponse> list = animalRepository
                .findAll(mappableAnimalSpec(), Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(animal -> toNearbyAnimal(animal, latitude, longitude))
                .filter(item -> item.distanceKm() <= safeDistance)
                .sorted(Comparator.comparing(LocationDtos.NearbyAnimalResponse::distanceKm))
                .toList();

        return new LocationDtos.NearbyAnimalListResponse(
                latitude,
                longitude,
                safeDistance,
                list.size(),
                list
        );
    }

    /**
     * 按距离筛选已认证救助站。
     */
    @Transactional(readOnly = true)
    public LocationDtos.NearbyRescueStationListResponse nearbyRescueStations(Double latitude, Double longitude, Double distance) {
        SecuritySupport.requireUser();
        double safeDistance = validateNearbyParams(latitude, longitude, distance);

        List<LocationDtos.NearbyRescueStationResponse> list = rescueStationRepository
                .findAll(mappableStationSpec(), Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(station -> toNearbyStation(station, latitude, longitude))
                .filter(item -> item.distanceKm() <= safeDistance)
                .sorted(Comparator.comparing(LocationDtos.NearbyRescueStationResponse::distanceKm))
                .toList();

        return new LocationDtos.NearbyRescueStationListResponse(
                latitude,
                longitude,
                safeDistance,
                list.size(),
                list
        );
    }

    private Specification<Animal> mappableAnimalSpec() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(VISIBLE_ANIMAL_STATUSES));
            predicates.add(cb.isNotNull(root.get("foundLatitude")));
            predicates.add(cb.isNotNull(root.get("foundLongitude")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<RescueStation> mappableStationSpec() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("certificationStatus"), CertificationStatus.APPROVED),
                cb.isNotNull(root.get("latitude")),
                cb.isNotNull(root.get("longitude"))
        );
    }

    private LocationDtos.NearbyAnimalResponse toNearbyAnimal(Animal animal, Double userLatitude, Double userLongitude) {
        double distanceKm = HaversineUtil.distanceKm(
                userLatitude,
                userLongitude,
                animal.getFoundLatitude(),
                animal.getFoundLongitude()
        );
        return new LocationDtos.NearbyAnimalResponse(
                animal.getId(),
                animal.getType().getLabel(),
                animal.getGender().getLabel(),
                animal.getAge(),
                animal.getFoundRegion(),
                animal.getFoundLatitude(),
                animal.getFoundLongitude(),
                animal.getStatus().getLabel(),
                animal.getCoverImageUrl(),
                distanceKm,
                animal.getCreatedAt()
        );
    }

    private LocationDtos.NearbyRescueStationResponse toNearbyStation(RescueStation station, Double userLatitude, Double userLongitude) {
        double distanceKm = HaversineUtil.distanceKm(
                userLatitude,
                userLongitude,
                station.getLatitude(),
                station.getLongitude()
        );
        return new LocationDtos.NearbyRescueStationResponse(
                station.getId(),
                station.getStationName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                station.getContactPhone(),
                station.getServiceTime(),
                distanceKm,
                station.getUpdatedAt()
        );
    }

    private LocationDtos.RescueStationLocationResponse toStationLocationResponse(RescueStation station) {
        return new LocationDtos.RescueStationLocationResponse(
                station.getId(),
                station.getStationName(),
                station.getLatitude(),
                station.getLongitude(),
                station.getAddress(),
                station.getUpdatedAt()
        );
    }

    private double validateNearbyParams(Double latitude, Double longitude, Double distance) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("经纬度不能为空");
        }
        validateLatitude(latitude);
        validateLongitude(longitude);

        double safeDistance = distance == null ? 5.0 : distance;
        if (!ALLOWED_DISTANCE_KM.contains(safeDistance)) {
            throw new BusinessException("距离参数只能是 1、3、5、10 km");
        }
        return safeDistance;
    }

    private void validateLatitude(Double latitude) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new BusinessException("纬度范围必须在 -90 到 90 之间");
        }
    }

    private void validateLongitude(Double longitude) {
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new BusinessException("经度范围必须在 -180 到 180 之间");
        }
    }
}
