package com.guitu.service;

import com.guitu.domain.enums.AnimalStatus;
import com.guitu.domain.enums.ApplyStatus;
import com.guitu.domain.enums.RescueStatus;
import com.guitu.domain.enums.UserStatus;
import com.guitu.dto.StatsDtos;
import com.guitu.repository.AdoptApplyRepository;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.RescueRepository;
import com.guitu.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatisticsService {
    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final RescueRepository rescueRepository;
    private final AdoptApplyRepository adoptApplyRepository;
    private final AnimalService animalService;
    private final RescueService rescueService;
    private final NoticeService noticeService;

    public StatisticsService(
            UserRepository userRepository,
            AnimalRepository animalRepository,
            RescueRepository rescueRepository,
            AdoptApplyRepository adoptApplyRepository,
            AnimalService animalService,
            RescueService rescueService,
            NoticeService noticeService
    ) {
        this.userRepository = userRepository;
        this.animalRepository = animalRepository;
        this.rescueRepository = rescueRepository;
        this.adoptApplyRepository = adoptApplyRepository;
        this.animalService = animalService;
        this.rescueService = rescueService;
        this.noticeService = noticeService;
    }

    @Cacheable("adminOverview")
    @Transactional(readOnly = true)
    public StatsDtos.OverviewResponse overview() {
        long pending = animalRepository.countByStatusIn(List.of(AnimalStatus.PENDING_REVIEW))
                + rescueRepository.countByStatusIn(List.of(RescueStatus.PENDING_REVIEW))
                + adoptApplyRepository.countByStatus(ApplyStatus.PENDING_REVIEW);
        return new StatsDtos.OverviewResponse(
                userRepository.countByStatus(UserStatus.NORMAL),
                animalRepository.count(),
                rescueRepository.count(),
                adoptApplyRepository.count(),
                pending
        );
    }

    @Cacheable("animalStatusDistribution")
    @Transactional(readOnly = true)
    public List<StatsDtos.StatusCount> animalStatusDistribution() {
        return animalRepository.countGroupByStatus().stream()
                .map(row -> {
                    AnimalStatus status = (AnimalStatus) row[0];
                    return new StatsDtos.StatusCount(status.name(), status.getLabel(), (Long) row[1]);
                })
                .toList();
    }

    @Cacheable("rescueStatusDistribution")
    @Transactional(readOnly = true)
    public List<StatsDtos.StatusCount> rescueStatusDistribution() {
        return rescueRepository.countGroupByStatus().stream()
                .map(row -> {
                    RescueStatus status = (RescueStatus) row[0];
                    return new StatsDtos.StatusCount(status.name(), status.getLabel(), (Long) row[1]);
                })
                .toList();
    }

    @Cacheable("applyStatusDistribution")
    @Transactional(readOnly = true)
    public List<StatsDtos.StatusCount> applyStatusDistribution() {
        return adoptApplyRepository.countGroupByStatus().stream()
                .map(row -> {
                    ApplyStatus status = (ApplyStatus) row[0];
                    return new StatsDtos.StatusCount(status.name(), status.getLabel(), (Long) row[1]);
                })
                .toList();
    }

    @Cacheable("homeOverview")
    @Transactional(readOnly = true)
    public StatsDtos.HomeOverviewResponse homeOverview() {
        return new StatsDtos.HomeOverviewResponse(
                animalService.latestPublic(6),
                rescueService.latestPublic(6),
                noticeService.latestPublic(5),
                overview()
        );
    }
}
