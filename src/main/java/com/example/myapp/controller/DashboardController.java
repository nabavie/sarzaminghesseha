package com.example.myapp.controller;

import com.example.myapp.dto.ChangePasswordForm;
import com.example.myapp.model.ListeningProgress;
import com.example.myapp.model.Role;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.CommentService;
import com.example.myapp.service.CustomUserDetailsService;
import com.example.myapp.service.FavoriteService;
import com.example.myapp.service.FileStorageService;
import com.example.myapp.service.PasswordRecoveryService;
import com.example.myapp.service.ProgressService;
import com.example.myapp.service.StorytellerRequestService;
import com.example.myapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;
    private final FavoriteService favoriteService;
    private final ProgressService progressService;
    private final StorytellerRequestService requestService;
    private final FileStorageService storage;
    private final PasswordRecoveryService recoveryService;
    private final CustomUserDetailsService userDetailsService;
    private final CommentService commentService;

    public DashboardController(UserService userService,
                               FavoriteService favoriteService,
                               ProgressService progressService,
                               StorytellerRequestService requestService,
                               FileStorageService storage,
                               PasswordRecoveryService recoveryService,
                               CustomUserDetailsService userDetailsService,
                               CommentService commentService) {
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.progressService = progressService;
        this.requestService = requestService;
        this.storage = storage;
        this.recoveryService = recoveryService;
        this.userDetailsService = userDetailsService;
        this.commentService = commentService;
    }

    @GetMapping
    public String index(Model model, Principal principal) {
        User user = currentUser(principal);
        model.addAttribute("favorites", favoriteService.findByUser(user).stream()
                .filter(f -> f.getTale().getStatus() == TaleStatus.APPROVED)
                .toList());

        List<ListeningProgress> progress = progressService.findByUser(user).stream()
                .filter(p -> p.getTale().getStatus() == TaleStatus.APPROVED)
                .toList();
        model.addAttribute("inProgress", progress.stream()
                .filter(p -> !p.isFinished() && p.getListenedSeconds() > 0)
                .toList());
        model.addAttribute("finished", progress.stream()
                .filter(ListeningProgress::isFinished)
                .toList());
        model.addAttribute("totalListenedSeconds", progress.stream()
                .mapToLong(ListeningProgress::getListenedSeconds)
                .sum());

        model.addAttribute("isStoryteller", user.hasRole(Role.STORYTELLER));
        model.addAttribute("latestRequest", requestService.latestForUser(user).orElse(null));
        if (user.hasRole(Role.STORYTELLER)) {
            model.addAttribute("storytellerComments", commentService.forStoryteller(user).stream().limit(8).toList());
            model.addAttribute("unseenOnDashboard", commentService.countUnseenForStoryteller(user));
        }
        return "dashboard/index";
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        model.addAttribute("user", currentUser(principal));
        if (model.getAttribute("passwordForm") == null) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        return "dashboard/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult result,
                                 Model model,
                                 Principal principal,
                                 RedirectAttributes redirect) {
        User user = currentUser(principal);
        if (!result.hasFieldErrors("currentPassword")
                && !userService.passwordMatches(user, form.getCurrentPassword())) {
            result.rejectValue("currentPassword", "wrong", "رمز عبور فعلی درست نیست");
        }
        if (!result.hasFieldErrors("confirmPassword")
                && form.getNewPassword() != null
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "رمز عبور تازه و تکرار آن یکسان نیستند");
        }
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "dashboard/profile";
        }
        userService.changePassword(user, form.getNewPassword());
        redirect.addFlashAttribute("success", "رمز عبور شما عوض شد ✔");
        return "redirect:/dashboard/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String displayName,
                                @RequestParam(required = false) MultipartFile avatar,
                                Principal principal,
                                RedirectAttributes redirect) {
        User user = currentUser(principal);
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) {
            redirect.addFlashAttribute("error", "نام نمی‌تواند خالی باشد");
            return "redirect:/dashboard/profile";
        }
        try {
            String avatarPath = null;
            if (avatar != null && !avatar.isEmpty()) {
                avatarPath = storage.storeAvatar(avatar);
                if (user.getAvatarPath() != null) {
                    storage.delete(FileStorageService.AVATARS, user.getAvatarPath());
                }
            }
            userService.updateProfile(user, name, avatarPath);
            redirect.addFlashAttribute("success", "پروفایل شما ذخیره شد ✔");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/profile";
    }

    @PostMapping("/storyteller-request")
    public String requestStoryteller(@RequestParam(required = false) String message,
                                     Principal principal,
                                     HttpServletRequest request,
                                     RedirectAttributes redirect) {
        User user = currentUser(principal);
        if (requestService.submit(user, message)) {
            refreshAuthorities(user.getUsername(), request);
            redirect.addFlashAttribute("success",
                    "🎉 تبریک! شما حالا قصه‌گوی سرزمین قصه‌ها هستید و می‌توانید قصه بفرستید.");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/recovery-code")
    public String regenerateRecoveryCode(Principal principal, RedirectAttributes redirect) {
        String code = recoveryService.issueCode(currentUser(principal));
        redirect.addFlashAttribute("newRecoveryCode", code);
        return "redirect:/dashboard/profile";
    }

    /**
     * Reloads the user's authorities into the current session so a freshly
     * granted role (e.g. STORYTELLER) works without logging out and back in.
     */
    private void refreshAuthorities(String username, HttpServletRequest request) {
        UserDetails details = userDetailsService.loadUserByUsername(username);
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        Authentication refreshed = UsernamePasswordAuthenticationToken.authenticated(
                current.getPrincipal(), current.getCredentials(), details.getAuthorities());
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(refreshed);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private User currentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
