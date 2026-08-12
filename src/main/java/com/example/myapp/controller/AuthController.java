package com.example.myapp.controller;

import com.example.myapp.dto.ForgotPasswordForm;
import com.example.myapp.dto.RegistrationForm;
import com.example.myapp.model.User;
import com.example.myapp.service.PasswordRecoveryService;
import com.example.myapp.service.UserService;
import com.example.myapp.util.MobileNumbers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordRecoveryService recoveryService;
    private final DaoAuthenticationProvider authenticationProvider;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService,
                          PasswordRecoveryService recoveryService,
                          DaoAuthenticationProvider authenticationProvider) {
        this.userService = userService;
        this.recoveryService = recoveryService;
        this.authenticationProvider = authenticationProvider;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
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
                form.setMobile(MobileNumbers.normalizeOptional(form.getMobile()));
            } catch (IllegalArgumentException e) {
                result.rejectValue("mobile", "invalid", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        User user = userService.register(form);
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
