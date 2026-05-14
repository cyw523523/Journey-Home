package com.guitu.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheInvalidationService {
    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictPublicCaches() {
        clear("homeOverview");
        clear("adminOverview");
        clear("animalStatusDistribution");
        clear("rescueStatusDistribution");
        clear("applyStatusDistribution");
        clear("latestAnimals");
        clear("latestRescues");
        clear("latestNotices");
    }

    private void clear(String cacheName) {
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }
    }
}
