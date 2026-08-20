package com.example.myapp.controller;

import com.example.myapp.model.User;
import com.example.myapp.service.LoginAttemptService;
import com.example.myapp.service.SessionLoginService;
import com.example.myapp.service.SmsIrClient;
import com.example.myapp.service.SmsOtpService;
import com.example.myapp.service.UserService;
import com.example.myapp.util.MobileNumbers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

/**
 * SMS login is a separate POST flow from Spring Security's username/password filter.
 * Always redirects back to {@code /login?sms} so the code form is a normal GET page.
 */
@Controller
@RequestMapping("/login/sms")
public class SmsLoginController {

    static final String SESSION_MOBILE = "smsLoginMobile";
    static final String SESSION_NAME = "smsLoginName";

    private static final Logger log = LoggerFactory.getLogger(SmsLoginController.class);

    private final UserService userService;
    private final SmsOtpService otpService;
    private final SmsIrClient smsIrClient;
    private final SessionLoginService sessionLogin;
    private final LoginAttemptService loginAttempts;

    public SmsLoginController(UserService userService,
                              SmsOtpService otpService,
                              SmsIrClient smsIrClient,
                              SessionLoginService sessionLogin,
                              LoginAttemptService loginAttempts) {
        this.userService = userService;
        this.otpService = otpService;
        this.smsIrClient = smsIrClient;
        this.sessionLogin = sessionLogin;
        this.loginAttempts = loginAttempts;
    }

    @GetMapping({"/send", "/verify"})
    public String getFallback() {
        return "redirect:/login?sms";
    }

    @GetMapping("/cancel")
    public String cancel(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_MOBILE);
            session.removeAttribute(SESSION_NAME);
        }
        return "redirect:/login?sms";
    }

    @PostMapping("/send")
    public String send(@RequestParam(required = false) String mobile,
                       Authentication authentication,
                       HttpServletRequest request,
                       RedirectAttributes redirect) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        String normalized;
        try {
            normalized = MobileNumbers.normalizeRequired(mobile);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("smsError", e.getMessage());
            return "redirect:/login?sms";
        }
        User user = userService.findActiveByMobile(normalized).orElse(null);
        if (user == null) {
            redirect.addFlashAttribute("smsError", "این شماره موبایل برای هیچ حساب فعالی ثبت نشده است");
            return "redirect:/login?sms";
        }
        try {
            String code = otpService.issue(normalized, request.getRemoteAddr());
            smsIrClient.sendOtp(normalized, user.getDisplayName(), code);
        } catch (SmsOtpService.TooSoonException e) {
            remember(request, normalized, user.getDisplayName());
            redirect.addFlashAttribute("smsError", "کد قبلی هنوز معتبر است؛ یک دقیقه صبر کنید و دوباره بخواهید");
            return "redirect:/login?sms";
        } catch (SmsOtpService.IpThrottledException e) {
            redirect.addFlashAttribute("smsError", "تعداد درخواست پیامک از این دستگاه زیاد بوده؛ کمی بعد دوباره تلاش کنید");
            return "redirect:/login?sms";
        } catch (SmsIrClient.SmsException e) {
            otpService.invalidate(normalized);
            log.warn("OTP SMS failed for {}: {}", SmsIrClient.mask(normalized), e.getMessage());
            redirect.addFlashAttribute("smsError", e.getMessage());
            return "redirect:/login?sms";
        } catch (RuntimeException e) {
            otpService.invalidate(normalized);
            log.warn("OTP SMS failed for {}: {}", SmsIrClient.mask(normalized), e.getMessage(), e);
            redirect.addFlashAttribute("smsError", "ارسال پیامک ممکن نشد: " + e.getMessage());
            return "redirect:/login?sms";
        }
        remember(request, normalized, user.getDisplayName());
        redirect.addFlashAttribute("smsInfo", "کد ورود به شمارهٔ شما پیامک شد. تا ۵ دقیقه فرصت دارید.");
        return "redirect:/login?sms";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam(required = false) String code,
                         Authentication authentication,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirect) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        HttpSession session = request.getSession(false);
        String mobile = session == null ? null : (String) session.getAttribute(SESSION_MOBILE);
        if (mobile == null) {
            redirect.addFlashAttribute("smsError", "ابتدا شماره موبایل را وارد کنید تا کد برایتان پیامک شود");
            return "redirect:/login?sms";
        }
        SmsOtpService.VerifyResult result = otpService.verify(mobile, code);
        return switch (result) {
            case EXPIRED -> {
                redirect.addFlashAttribute("smsExpired", true);
                redirect.addFlashAttribute("smsError", "زمان این کد به پایان رسیده است. دوباره کد بگیرید.");
                yield "redirect:/login?sms";
            }
            case WRONG -> {
                redirect.addFlashAttribute("smsError", "کد واردشده درست نیست");
                yield "redirect:/login?sms";
            }
            case TOO_MANY, MISSING -> {
                session.removeAttribute(SESSION_MOBILE);
                redirect.addFlashAttribute("smsError", "این کد دیگر معتبر نیست. دوباره کد بگیرید.");
                yield "redirect:/login?sms";
            }
            case OK -> {
                User user = userService.findActiveByMobile(mobile).orElse(null);
                if (user == null) {
                    redirect.addFlashAttribute("smsError", "این شماره موبایل برای هیچ حساب فعالی ثبت نشده است");
                    yield "redirect:/login?sms";
                }
                try {
                    sessionLogin.login(user, request, response);
                    loginAttempts.clearFailures(request.getRemoteAddr(), user.getUsername());
                    session.removeAttribute(SESSION_MOBILE);
                    session.removeAttribute(SESSION_NAME);
                    yield "redirect:/dashboard";
                } catch (DisabledException e) {
                    yield "redirect:/login?disabled";
                }
            }
        };
    }

    static void populateCodeStep(Model model, String mobile, Instant expiresAt) {
        model.addAttribute("smsAwaitingCode", true);
        model.addAttribute("smsMobile", mobile);
        model.addAttribute("smsMaskedMobile", mask(mobile));
        if (expiresAt != null) {
            model.addAttribute("smsExpiresAtMillis", expiresAt.toEpochMilli());
        }
    }

    static String mask(String mobile) {
        if (mobile == null || mobile.length() < 8) {
            return mobile;
        }
        return mobile.substring(0, 4) + "***" + mobile.substring(mobile.length() - 2);
    }

    private static void remember(HttpServletRequest request, String mobile, String name) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_MOBILE, mobile);
        session.setAttribute(SESSION_NAME, name);
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
