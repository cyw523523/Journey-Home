package com.guitu.dto;

import java.util.List;

public final class StatsDtos {
    private StatsDtos() {
    }

    public record OverviewResponse(
            long userCount,
            long animalCount,
            long rescueCount,
            long applyCount,
            long pendingAuditCount
    ) {
    }

    public record StatusCount(String status, String statusText, long count) {
    }

    public record HomeOverviewResponse(
            List<AnimalDtos.AnimalResponse> latestAnimals,
            List<RescueDtos.RescueResponse> latestRescues,
            List<NoticeDtos.NoticeResponse> latestNotices,
            OverviewResponse overview
    ) {
    }
}
