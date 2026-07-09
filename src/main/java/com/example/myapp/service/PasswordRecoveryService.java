package com.example.myapp.service;

import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Password recovery via a one-time recovery code: no e-mail infrastructure needed,
 * but still safe — the code is long random, stored only as a BCrypt hash,
 * consumed on use, and guesses are throttled per username.
 */
@Service
public class PasswordRecoveryService {

    /** No 0/O/1/I/L to keep hand-copied codes unambiguous. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 16;
    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public PasswordRecoveryService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generates a fresh recovery code for the user and stores only its hash.
     * Returns the plain code — the single moment it is visible; show it once.
     */
    @Transactional
    public String issueCode(User user) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH + 3);
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        String code = sb.toString();
        user.setRecoveryCodeHash(passwordEncoder.encode(normalize(code)));
        userRepository.save(user);
        return code;
    }

    /**
     * Verifies the recovery code and sets the new password. The used code is
     * consumed and a new one issued and returned (to show once).
     *
     * @throws IllegalArgumentException with a Persian message on any failure
     */
    @Transactional
    public String resetPassword(String username, String code, String newPassword) {
        String key = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (isThrottled(key)) {
            throw new IllegalArgumentException("تلاش‌های زیادی انجام شده؛ لطفاً ۱۵ دقیقه بعد دوباره امتحان کنید");
        }
        User user = userRepository.findByUsername(username == null ? "" : username.trim()).orElse(null);
        if (user == null || user.getRecoveryCodeHash() == null
                || !passwordEncoder.matches(normalize(code), user.getRecoveryCodeHash())) {
            recordFailure(key);
            throw new IllegalArgumentException("نام کاربری یا کد بازیابی درست نیست");
        }
        failures.remove(key);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // the old code is now consumed; hand the user a fresh one
        return issueCode(user);
    }

    private String normalize(String code) {
        return code == null ? "" : code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private boolean isThrottled(String key) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return false;
        }
        Instant cutoff = Instant.now().minus(FAILURE_WINDOW);
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }
            return attempts.size() >= MAX_FAILURES;
        }
    }

    private void recordFailure(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(Instant.now());
        }
    }
}
