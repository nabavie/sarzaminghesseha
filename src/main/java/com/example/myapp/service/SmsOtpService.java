package com.example.myapp.service;

import com.example.myapp.util.MobileNumbers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory one-time codes for SMS login. No schema change — production-safe.
 * Codes last {@link #TTL}, are hashed, and die after too many guesses.
 */
@Service
public class SmsOtpService {

    public static final Duration TTL = Duration.ofMinutes(5);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MAX_SENDS_PER_IP_PER_HOUR = 8;

    public enum VerifyResult {
        OK, EXPIRED, WRONG, TOO_MANY, MISSING
    }

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, SendWindow> sendsByIp = new ConcurrentHashMap<>();

    public SmsOtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String issue(String mobile09, String ip) {
        purgeExpired();
        Challenge existing = challenges.get(mobile09);
        if (existing != null && existing.lastSentAt.plus(RESEND_COOLDOWN).isAfter(Instant.now())) {
            throw new TooSoonException();
        }
        if (isIpThrottled(ip)) {
            throw new IpThrottledException();
        }
        String code = String.format("%05d", random.nextInt(100_000));
        Instant now = Instant.now();
        challenges.put(mobile09, new Challenge(passwordEncoder.encode(code), now.plus(TTL), now, 0));
        recordIpSend(ip, now);
        return code;
    }

    public Instant expiresAt(String mobile09) {
        Challenge challenge = challenges.get(mobile09);
        return challenge == null ? null : challenge.expiresAt;
    }

    public VerifyResult verify(String mobile09, String rawCode) {
        Challenge challenge = challenges.get(mobile09);
        if (challenge == null) {
            return VerifyResult.MISSING;
        }
        Instant now = Instant.now();
        if (!now.isBefore(challenge.expiresAt)) {
            challenges.remove(mobile09);
            return VerifyResult.EXPIRED;
        }
        if (challenge.attempts >= MAX_VERIFY_ATTEMPTS) {
            challenges.remove(mobile09);
            return VerifyResult.TOO_MANY;
        }
        String code = MobileNumbers.toAsciiDigits(rawCode == null ? "" : rawCode.trim());
        challenge.attempts++;
        if (code.length() != 5 || !passwordEncoder.matches(code, challenge.codeHash)) {
            if (challenge.attempts >= MAX_VERIFY_ATTEMPTS) {
                challenges.remove(mobile09);
                return VerifyResult.TOO_MANY;
            }
            return VerifyResult.WRONG;
        }
        challenges.remove(mobile09);
        return VerifyResult.OK;
    }

    public void invalidate(String mobile09) {
        if (mobile09 != null) {
            challenges.remove(mobile09);
        }
    }

    private boolean isIpThrottled(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        SendWindow window = sendsByIp.get(ip);
        if (window == null) {
            return false;
        }
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        window.prune(cutoff);
        return window.count() >= MAX_SENDS_PER_IP_PER_HOUR;
    }

    private void recordIpSend(String ip, Instant now) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        sendsByIp.computeIfAbsent(ip, k -> new SendWindow()).add(now);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(e -> !now.isBefore(e.getValue().expiresAt.plus(Duration.ofMinutes(10))));
        Instant cutoff = now.minus(Duration.ofHours(1));
        sendsByIp.values().forEach(w -> w.prune(cutoff));
        sendsByIp.entrySet().removeIf(e -> e.getValue().count() == 0);
    }

    private static final class Challenge {
        private final String codeHash;
        private final Instant expiresAt;
        private final Instant lastSentAt;
        private int attempts;

        private Challenge(String codeHash, Instant expiresAt, Instant lastSentAt, int attempts) {
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
            this.lastSentAt = lastSentAt;
            this.attempts = attempts;
        }
    }

    private static final class SendWindow {
        private final java.util.ArrayDeque<Instant> times = new java.util.ArrayDeque<>();

        synchronized void add(Instant t) {
            times.addLast(t);
        }

        synchronized void prune(Instant cutoff) {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                times.removeFirst();
            }
        }

        synchronized int count() {
            return times.size();
        }
    }

    public static class TooSoonException extends RuntimeException {
    }

    public static class IpThrottledException extends RuntimeException {
    }
}
