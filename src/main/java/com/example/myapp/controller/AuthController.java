package com.example.myapp.controller;

import com.example.myapp.dto.ForgotPasswordForm;
import com.example.myapp.dto.RegistrationForm;
import com.example.myapp.model.User;
import com.example.myapp.service.PasswordRecoveryService;
import com.example.myapp.service.SmsIrClient;
import com.example.myapp.service.SmsOtpService;
import com.example.myapp.service.UserService;
import com.example.myapp.util.MobileNumbers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final PasswordRecoveryService recoveryService;
    private final DaoAuthenticationProvider authenticationProvider;
    private final SmsIrClient smsIrClient;
    private final SmsOtpService otpService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService,
                          PasswordRecoveryService recoveryService,
                          DaoAuthenticationProvider authenticationProvider,
                          SmsIrClient smsIrClient,
                          SmsOtpService otpService) {
        this.userService = userService;
        this.recoveryService = recoveryService;
        this.authenticationProvider = authenticationProvider;
        this.smsIrClient = smsIrClient;
        this.otpService = otpService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication,
                        HttpServletRequest request,
                        Model model,
                        @RequestParam(required = false) String sms,
                        @RequestParam(required = false) String mode) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        boolean smsTab = !"password".equals(mode) && sms != null;
        model.addAttribute("smsTab", smsTab);
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object pending = session.getAttribute(SmsLoginController.SESSION_MOBILE);
            if (pending instanceof String mobile && !mobile.isBlank()) {
                SmsLoginController.populateCodeStep(model, mobile, otpService.expiresAt(mobile));
            }
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("form") RegistrationForm form,
                               Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        return "auth/register";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult result,
                           RedirectAttributes redirect,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (!result.hasFieldErrors("username") && userService.usernameExists(form.getUsername().trim())) {
            result.rejectValue("username", "duplicate", "این نام کاربری قبلاً گرفته شده است؛ یکی دیگر انتخاب کنید");
        }
        if (!result.hasFieldErrors("confirmPassword")
                && form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "رمز عبور و تکرار آن یکسان نیستند");
        }
        if (!result.hasFieldErrors("mobile")) {
            try {
                form.setMobile(MobileNumbers.normalizeRequired(form.getMobile()));
            } catch (IllegalArgumentException e) {
                result.rejectValue("mobile", "invalid", e.getMessage());
            }
        }
        if (!result.hasFieldErrors("mobile") && form.getMobile() != null
                && userService.mobileTakenByActiveUser(form.getMobile())) {
            result.rejectValue("mobile", "duplicate",
                    "این شماره موبایل قبلاً برای یک حساب فعال ثبت شده است");
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        User user = userService.register(form);
        sendWelcomeSms(user, redirect);
        // shown exactly once on the next page; only the hash is stored
        redirect.addFlashAttribute("recoveryCode", recoveryService.issueCode(user));
        signIn(user.getUsername(), form.getPassword(), request, response);
        return "redirect:/register/recovery-code";
    }

    /**
     * Logs a freshly registered user in so they don't have to retype credentials they
     * just chose. Goes through the same provider as the login form, so the account
     * checks in {@code SecurityConfig} still apply; a failure here is not fatal, the
     * visitor simply lands on the recovery-code page as a guest and can log in manually.
     */
    private void signIn(String username, String rawPassword,
                        HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationProvider.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, rawPassword));
            if (authentication instanceof CredentialsContainer container) {
                container.eraseCredentials();
            }
            // rotate the id before the context is stored, so a pre-registration
            // session handed to the visitor cannot be reused
            request.getSession(true);
            request.changeSessionId();

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
        }
    }

    /** Welcome SMS must never roll back a successful registration. */
    private void sendWelcomeSms(User user, RedirectAttributes redirect) {
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            return;
        }
        try {
            smsIrClient.sendWelcome(user.getMobile(), user.getDisplayName());
            log.info("Welcome SMS sent to {} ({})", user.getUsername(),
                    SmsIrClient.mask(user.getMobile()));
        } catch (RuntimeException e) {
            log.warn("Welcome SMS failed for user {} ({}): {}",
                    user.getUsername(), SmsIrClient.mask(user.getMobile()), e.getMessage(), e);
            redirect.addFlashAttribute("smsWarning",
                    "ثبت‌نام انجام شد، اما پیامک خوش‌آمد ارسال نشد: " + e.getMessage());
        }
    }

    /** One-time display of the recovery code right after registration (flash-scoped). */
    @GetMapping("/register/recovery-code")
    public String recoveryCode(org.springframework.ui.Model model) {
        Object code = model.getAttribute("recoveryCode");
        if (code == null || code.toString().isBlank()) {
            return "redirect:/login";
        }
        return "auth/recovery-code";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(@ModelAttribute("form") ForgotPasswordForm form) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("form") ForgotPasswordForm form,
                                 BindingResult result,
                                 RedirectAttributes redirect) {
        if (!result.hasFieldErrors("confirmPassword")
                && form.getNewPassword() != null
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "رمز عبور و تکرار آن یکسان نیستند");
        }
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            String newCode = recoveryService.resetPassword(
                    form.getUsername(), form.getRecoveryCode(), form.getNewPassword());
            redirect.addFlashAttribute("recoveryCode", newCode);
            redirect.addFlashAttribute("afterReset", true);
            return "redirect:/register/recovery-code";
        } catch (IllegalArgumentException e) {
            result.reject("recovery", e.getMessage());
            return "auth/forgot-password";
        }
    }
}
