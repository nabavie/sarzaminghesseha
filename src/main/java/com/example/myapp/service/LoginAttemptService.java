package com.example.myapp.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory login brute-force protection. Failed attempts are tracked per client IP
 * AND per target username (so both a single attacker spraying many accounts and a
 * distributed attack on one account are caught — anonymous clients are always
 * covered by the IP key). Reaching {@value #MAX_FAILURES} failures within
 * {@link #WINDOW} bans that key from logging in for {@link #BAN_DURATION}.
 * State is in-memory and resets on restart.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration BAN_DURATION = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Map<String, Instant> bannedUntil = new ConcurrentHashMap<>();

    public boolean isBanned(String ip, String username) {
        return isKeyBanned(ipKey(ip)) || isKeyBanned(userKey(username));
    }

    public void recordFailure(String ip, String username) {
        recordKey(ipKey(ip));
        recordKey(userKey(username));
    }

    public void clearFailures(String ip, String username) {
        String ipKey = ipKey(ip);
        String userKey = userKey(username);
        if (ipKey != null) {
            failures.remove(ipKey);
        }
        if (userKey != null) {
            failures.remove(userKey);
        }
    }

    /** A successful login wipes the failure history for that IP and username (not active bans). */
    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String ip = event.getAuthentication().getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress() : null;
        clearFailures(ip, event.getAuthentication().getName());
    }

    private void recordKey(String key) {
        if (key == null) {
            return;
        }
        Instant now = Instant.now();
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        attempts.addLast(now);
        Instant cutoff = now.minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
        if (attempts.size() >= MAX_FAILURES) {
            bannedUntil.put(key, now.plus(BAN_DURATION));
            failures.remove(key);
        }
    }

    private boolean isKeyBanned(String key) {
        if (key == null) {
            return false;
        }
        Instant until = bannedUntil.get(key);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            bannedUntil.remove(key);
            return false;
        }
        return true;
    }

    private String ipKey(String ip) {
        return ip == null || ip.isBlank() ? null : "ip:" + ip;
    }

    private String userKey(String username) {
        return username == null || username.isBlank() ? null : "user:" + username.trim().toLowerCase();
    }
}
