package com.example.myapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMS.ir settings. Secrets come from {@code SMS_API_KEY} / {@code app.sms.api-key}.
 */
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private String apiKey = "";
    private String verifyUrl = "https://api.sms.ir/v1/send/verify";
    private String bulkUrl = "https://api.sms.ir/v1/send/bulk";
    private long lineNumber = 3000498352L;
    private int otpTemplateId = 545384;
    private int welcomeTemplateId = 221192;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getVerifyUrl() {
        return verifyUrl;
    }

    public void setVerifyUrl(String verifyUrl) {
        this.verifyUrl = verifyUrl;
    }

    public String getBulkUrl() {
        return bulkUrl;
    }

    public void setBulkUrl(String bulkUrl) {
        this.bulkUrl = bulkUrl;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getOtpTemplateId() {
        return otpTemplateId;
    }

    public void setOtpTemplateId(int otpTemplateId) {
        this.otpTemplateId = otpTemplateId;
    }

    public int getWelcomeTemplateId() {
        return welcomeTemplateId;
    }

    public void setWelcomeTemplateId(int welcomeTemplateId) {
        this.welcomeTemplateId = welcomeTemplateId;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }
}
