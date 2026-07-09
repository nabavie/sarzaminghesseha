package com.example.myapp.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Short-lived HMAC tokens for the audio streaming endpoint, so tale audio has
 * no stable public URL that can be shared or hotlinked for download. Tokens are
 * minted into the page next to the player and expire after {@link #VALIDITY}.
 *
 * Named "mediaToken" so templates can mint tokens via {@code @mediaToken.issue(id)}.
 * Note: this stops link-sharing/hotlinking; nothing can stop a determined user
 * from capturing audio they are allowed to hear.
 */
@Service("mediaToken")
public class MediaTokenService {

    private static final Duration VALIDITY = Duration.ofHours(6);

    private final String configuredSecret;
    private byte[] secret;

    public MediaTokenService(@Value("${app.media.token-secret:}") String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    @PostConstruct
    void initSecret() {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            // random per boot: restarting just means pages mint fresh tokens
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
        }
    }

    /** Token format: {@code <expiryEpochSeconds>.<base64url HMAC of "taleId:expiry">}. */
    public String issue(Long taleId) {
        long expiry = Instant.now().plus(VALIDITY).getEpochSecond();
        return expiry + "." + sign(taleId, expiry);
    }

    public boolean isValid(Long taleId, String token) {
        if (taleId == null || token == null) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot <= 0) {
            return false;
        }
        long expiry;
        try {
            expiry = Long.parseLong(token.substring(0, dot));
        } catch (NumberFormatException e) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiry) {
            return false;
        }
        byte[] expected = sign(taleId, expiry).getBytes(StandardCharsets.UTF_8);
        byte[] actual = token.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private String sign(Long taleId, long expiry) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal((taleId + ":" + expiry).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }
}
