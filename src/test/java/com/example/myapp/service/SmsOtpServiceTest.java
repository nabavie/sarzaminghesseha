package com.example.myapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmsOtpServiceTest {

    private final PasswordEncoder encoder = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return "h:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals("h:" + rawPassword);
        }
    };

    @Test
    void expiredCodeIsRejected() {
        SmsOtpService service = new SmsOtpService(encoder);
        String code = service.issue("09123456789", "127.0.0.1");
        service.invalidate("09123456789");
        assertEquals(SmsOtpService.VerifyResult.MISSING, service.verify("09123456789", code));
    }

    @Test
    void matchingCodeSucceeds() {
        SmsOtpService service = new SmsOtpService(encoder);
        String code = service.issue("09123456789", "127.0.0.1");
        assertEquals(SmsOtpService.VerifyResult.OK, service.verify("09123456789", code));
    }

    @Test
    void wrongCodeFails() {
        SmsOtpService service = new SmsOtpService(encoder);
        service.issue("09123456789", "127.0.0.1");
        assertEquals(SmsOtpService.VerifyResult.WRONG, service.verify("09123456789", "00000"));
    }
}
