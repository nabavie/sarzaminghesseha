package com.example.myapp.service;

import com.example.myapp.config.SmsProperties;
import com.example.myapp.util.MobileNumbers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SMS.ir verify + bulk via JDK HttpClient so the payload matches their samples
 * (JSON + {@code x-api-key} + {@code Accept: text/plain}) without converter issues.
 */
@Service
public class SmsIrClient {

    private static final Logger log = LoggerFactory.getLogger(SmsIrClient.class);
    private static final int BULK_BATCH = 100;

    private final SmsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SmsIrClient(SmsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public void sendOtp(String mobile09, String displayName, String code) {
        sendVerify(mobile09, properties.getOtpTemplateId(), List.of(
                param("CODE", code),
                param("NAME", safeName(displayName))));
    }

    public void sendWelcome(String mobile09, String displayName) {
        sendVerify(mobile09, properties.getWelcomeTemplateId(), List.of(
                param("NAME", safeName(displayName))));
    }

    public void sendBulk(String messageText, Collection<String> mobiles09) {
        if (messageText == null || messageText.isBlank()) {
            throw new SmsException("متن پیامک خالی است");
        }
        List<String> mobiles = mobiles09.stream()
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .toList();
        if (mobiles.isEmpty()) {
            throw new SmsException("هیچ شماره موبایلی برای ارسال انتخاب نشده است");
        }
        requireConfigured();
        for (int i = 0; i < mobiles.size(); i += BULK_BATCH) {
            List<String> batch = mobiles.subList(i, Math.min(i + BULK_BATCH, mobiles.size()));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("lineNumber", properties.getLineNumber());
            body.put("messageText", messageText);
            body.put("mobiles", batch);
            body.put("sendDateTime", null);
            post(properties.getBulkUrl(), body, "bulk");
        }
    }

    private void sendVerify(String mobile09, int templateId, List<Map<String, String>> parameters) {
        requireConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mobile", MobileNumbers.toSmsIr(mobile09));
        body.put("templateId", templateId);
        body.put("parameters", parameters);
        log.info("SMS.ir verify templateId={} apiMobile={}", templateId, mask((String) body.get("mobile")));
        post(properties.getVerifyUrl(), body, "verify");
    }

    private void post(String url, Map<String, Object> body, String kind) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new SmsException("آماده‌سازی پیامک ناموفق بود");
        }
        log.info("SMS.ir {} POST {}", kind, url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "text/plain")
                .header("x-api-key", properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SMS.ir {} interrupted", kind);
            throw new SmsException("ارسال پیامک قطع شد؛ دوباره تلاش کنید");
        } catch (Exception e) {
            log.warn("SMS.ir {} network error: {}", kind, e.getMessage(), e);
            throw new SmsException("ارتباط با سرویس پیامک برقرار نشد: " + e.getMessage());
        }
        String raw = httpResponse.body();
        log.info("SMS.ir {} HTTP {} body={}", kind, httpResponse.statusCode(), raw);
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new SmsException(messageFromBody(raw,
                    "ارسال پیامک ناموفق بود (HTTP " + httpResponse.statusCode() + ")"));
        }
        if (!isSuccess(raw)) {
            throw new SmsException(messageFromBody(raw, "ارسال پیامک ناموفق بود"));
        }
    }

    private boolean isSuccess(String raw) {
        JsonNode root = tree(raw);
        if (root == null || !root.has("status")) {
            return false;
        }
        JsonNode status = root.get("status");
        return status.isNumber() ? status.asInt() == 1 : "1".equals(status.asText());
    }

    private String messageFromBody(String raw, String fallback) {
        JsonNode root = tree(raw);
        if (root != null && root.hasNonNull("message")) {
            String message = root.get("message").asText();
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return fallback;
    }

    private JsonNode tree(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("SMS.ir response was not JSON: {}", raw);
            return null;
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new SmsException("سرویس پیامک پیکربندی نشده است");
        }
    }

    private static Map<String, String> param(String name, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

    private static String safeName(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) {
            return "دوست قصه";
        }
        return name.length() > 40 ? name.substring(0, 40) : name;
    }

    public static String mask(String mobile09) {
        if (mobile09 == null || mobile09.length() < 6) {
            return mobile09;
        }
        return mobile09.substring(0, 4) + "***" + mobile09.substring(mobile09.length() - 2);
    }

    public static class SmsException extends RuntimeException {
        public SmsException(String message) {
            super(message);
        }
    }
}
