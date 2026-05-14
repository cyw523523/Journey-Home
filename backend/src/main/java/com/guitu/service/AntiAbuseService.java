package com.guitu.service;

import com.guitu.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AntiAbuseService {
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void check(String scope, String actorKey, int limit, Duration window, String message) {
        long now = System.currentTimeMillis();
        String key = scope + ":" + actorKey;
        Deque<Long> deque = buckets.computeIfAbsent(key, unused -> new ArrayDeque<>());
        synchronized (deque) {
            long threshold = now - window.toMillis();
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new BusinessException(message);
            }
            deque.addLast(now);
        }
    }
}
