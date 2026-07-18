package com.example.myapp.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Lightweight in-memory rate limit for the public feedback form (resets on restart).
 */
@Service
public class FeedbackRateLimitService {

    private static final int MAX_SUBMISSIONS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> submissions = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        Deque<Instant> attempts = submissions.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
        return attempts.size() < MAX_SUBMISSIONS;
    }

    public void record(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Deque<Instant> attempts = submissions.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        attempts.addLast(now);
        Instant cutoff = now.minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
