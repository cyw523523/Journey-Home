package com.guitu.service;

import com.guitu.domain.Animal;
import com.guitu.domain.RescueStation;
import com.guitu.domain.User;
import com.guitu.domain.UserLocationHistory;
import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.CertificationStatus;
import com.guitu.dto.MapDtos;
import com.guitu.exception.BusinessException;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.RescueStationRepository;
import com.guitu.repository.UserLocationHistoryRepository;
import com.guitu.security.SecuritySupport;
import com.guitu.util.HaversineUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 地图聚合查询服务。
 */
@Service
public class MapService {
    private static final List<AnimalStatus> MAP_VISIBLE_ANIMAL_STATUSES = List.of(
            AnimalStatus.WAITING_RESCUE,
            AnimalStatus.RESCUING,
            AnimalStatus.WAITING_ADOPTION,
            AnimalStatus.ADOPTED
    );

    private final AnimalRepository animalRepository;
    private final RescueStationRepository rescueStationRepository;
    private final UserService userService;
    private final UserLocationHistoryRepository userLocationHistoryRepository;

    public MapService(
            AnimalRepository animalRepository,
            RescueStationRepository rescueStationRepository,
            UserService userService,
            UserLocationHistoryRepository userLocationHistoryRepository
    ) {
        this.animalRepository = animalRepository;
        this.rescueStationRepository = rescueStationRepository;
        this.userService = userService;
        this.userLocationHistoryRepository = userLocationHistoryRepository;
    }

    @Transactional
    public List<MapDtos.MapPointResponse> listPoints(String pointType, Double latitude, Double longitude, Double radiusKm, Integer limit) {
        SecuritySupport.requireUser();
        validateQueryCoordinate(latitude, longitude);
        recordUserLocationIfPresent(latitude, longitude);
        String safeType = pointType == null || pointType.isBlank() ? "ALL" : pointType.trim().toUpperCase(Locale.ROOT);
        List<MapDtos.MapPointResponse> points = new ArrayList<>();
        if ("ALL".equals(safeType) || "ANIMAL".equals(safeType)) {
            points.addAll(nearbyAnimals(latitude, longitude, radiusKm, limit));
        }
        if ("ALL".equals(safeType) || "STATION".equals(safeType)) {
            points.addAll(nearbyStations(latitude, longitude, radiusKm, limit));
        }
        return points.stream()
                .sorted(pointComparator(latitude, longitude))
                .limit(safeLimit(limit))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapDtos.MapPointResponse> nearbyAnimals(Double latitude, Double longitude, Double radiusKm, Integer limit) {
        SecuritySupport.requireUser();
        validateQueryCoordinate(latitude, longitude);
        List<Animal> animals = animalRepository.findAll(mappableAnimalSpec(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return animals.stream()
                .map(animal -> toAnimalPoint(animal, latitude, longitude))
                .filter(point -> inRadius(point.distanceKm(), radiusKm))
                .sorted(pointComparator(latitude, longitude))
                .limit(safeLimit(limit))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapDtos.MapPointResponse> nearbyStations(Double latitude, Double longitude, Double radiusKm, Integer limit) {
        SecuritySupport.requireUser();
        validateQueryCoordinate(latitude, longitude);
        return rescueStationRepository.findAll(mappableStationSpec(), Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(station -> toStationPoint(station, latitude, longitude))
                .filter(point -> inRadius(point.distanceKm(), radiusKm))
                .sorted(pointComparator(latitude, longitude))
                .limit(safeLimit(limit))
                .toList();
    }

    @Transactional
    public MapDtos.NearbyResponse around(Double latitude, Double longitude, Double radiusKm, Integer limit) {
        SecuritySupport.requireUser();
        validateRequiredCoordinate(latitude, longitude);
        recordUserLocationIfPresent(latitude, longitude);
        return new MapDtos.NearbyResponse(
                nearbyAnimals(latitude, longitude, radiusKm, limit),
                nearbyStations(latitude, longitude, radiusKm, limit)
        );
    }

    @Transactional(readOnly = true)
    public List<MapDtos.ShelterStationResponse> listPublicStations(Double latitude, Double longitude, Double radiusKm) {
        SecuritySupport.requireUser();
        validateQueryCoordinate(latitude, longitude);
        return rescueStationRepository.findAll(mappableStationSpec(), Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(station -> toStationResponse(station, latitude, longitude))
                .filter(station -> inRadius(station.distanceKm(), radiusKm))
                .sorted(Comparator.comparing(MapDtos.ShelterStationResponse::distanceKm, Comparator.nullsLast(Double::compareTo)))
                .toList();
    }

    private Specification<Animal> mappableAnimalSpec() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(MAP_VISIBLE_ANIMAL_STATUSES));
            predicates.add(cb.isNotNull(root.get("foundLongitude")));
            predicates.add(cb.isNotNull(root.get("foundLatitude")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<RescueStation> mappableStationSpec() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("certificationStatus"), CertificationStatus.APPROVED),
                cb.isNotNull(root.get("longitude")),
                cb.isNotNull(root.get("latitude"))
        );
    }

    private MapDtos.MapPointResponse toAnimalPoint(Animal animal, Double userLatitude, Double userLongitude) {
        Double distance = distanceIfPossible(userLatitude, userLongitude, animal.getFoundLatitude(), animal.getFoundLongitude());
        return new MapDtos.MapPointResponse(
                animal.getId(),
                "ANIMAL",
                animal.getType().getLabel() + " · " + animal.getGender().getLabel(),
                animal.getFoundRegion(),
                animal.getFoundLongitude(),
                animal.getFoundLatitude(),
                animal.getDescription(),
                animal.getCoverImageUrl(),
                animal.getStatus().getLabel(),
                distance,
                "/animals/" + animal.getId(),
                animal.getCreatedAt()
        );
    }

    private MapDtos.MapPointResponse toStationPoint(RescueStation station, Double userLatitude, Double userLongitude) {
        Double distance = distanceIfPossible(userLatitude, userLongitude, station.getLatitude(), station.getLongitude());
        return new MapDtos.MapPointResponse(
                station.getId(),
                "STATION",
                station.getStationName(),
                station.getAddress(),
                station.getLongitude(),
                station.getLatitude(),
                station.getDescription(),
                station.getImageUrl(),
                station.getCertificationStatus().getLabel(),
                distance,
                station.getUser() == null ? null : "/users/" + station.getUser().getId(),
                station.getCreatedAt()
        );
    }

    private MapDtos.ShelterStationResponse toStationResponse(RescueStation station, Double userLatitude, Double userLongitude) {
        return new MapDtos.ShelterStationResponse(
                station.getId(),
                station.getStationName(),
                station.getAddress(),
                station.getLongitude(),
                station.getLatitude(),
                station.getContactPhone(),
                station.getServiceTime(),
                station.getDescription(),
                station.getCertificationStatus() == CertificationStatus.APPROVED,
                station.getUser() == null ? null : station.getUser().getId(),
                station.getUser() == null ? null : station.getUser().getNickname(),
                distanceIfPossible(userLatitude, userLongitude, station.getLatitude(), station.getLongitude()),
                station.getCreatedAt(),
                station.getUpdatedAt()
        );
    }

    private Comparator<MapDtos.MapPointResponse> pointComparator(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return Comparator.comparing(MapDtos.MapPointResponse::distanceKm, Comparator.nullsLast(Double::compareTo));
        }
        return (left, right) -> {
            if (left.createdAt() == null && right.createdAt() == null) {
                return 0;
            }
            if (left.createdAt() == null) {
                return 1;
            }
            if (right.createdAt() == null) {
                return -1;
            }
            return right.createdAt().compareTo(left.createdAt());
        };
    }

    private Double distanceIfPossible(Double userLatitude, Double userLongitude, Double targetLatitude, Double targetLongitude) {
        if (userLatitude == null || userLongitude == null || targetLatitude == null || targetLongitude == null) {
            return null;
        }
        return HaversineUtil.distanceKm(userLatitude, userLongitude, targetLatitude, targetLongitude);
    }

    private void recordUserLocationIfPresent(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }
        User user = userService.currentUser();
        UserLocationHistory history = new UserLocationHistory();
        history.setUser(user);
        history.setLatitude(latitude);
        history.setLongitude(longitude);
        userLocationHistoryRepository.save(history);
    }

    private boolean inRadius(Double distanceKm, Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            return true;
        }
        return distanceKm != null && distanceKm <= radiusKm;
    }

    private void validateQueryCoordinate(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException("经纬度需要同时传入");
        }
        if (latitude != null) {
            validateRequiredCoordinate(latitude, longitude);
        }
    }

    private void validateRequiredCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("请先授权当前位置或手动传入经纬度");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException("经纬度范围不合法");
        }
    }

    private long safeLimit(Integer limit) {
        if (limit == null) {
            return 100L;
        }
        return Math.max(1, Math.min(limit, 200));
    }
}
