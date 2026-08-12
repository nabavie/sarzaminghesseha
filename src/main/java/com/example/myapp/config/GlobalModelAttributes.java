package com.example.myapp.config;

import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.CommentService;
import com.example.myapp.service.FileStorageService;
import com.example.myapp.util.SiteUrl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    public static final String DEFAULT_DESCRIPTION =
            "سرزمین قصه‌ها — قصه صوتی، قصه شب، داستان کودکانه و نوجوانانه، و قصه گویی. "
                    + "قصه‌های صوتی برای کودکان، نوجوانان و همهٔ دوستداران قصه.";

    private final UserRepository userRepository;
    private final CommentService commentService;
    private final SiteUrl siteUrl;
    private final FileStorageService storage;

    public GlobalModelAttributes(UserRepository userRepository,
                                 CommentService commentService,
                                 SiteUrl siteUrl,
                                 FileStorageService storage) {
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.siteUrl = siteUrl;
        this.storage = storage;
    }

    /** Upload ceilings, so file inputs can warn before the browser sends the bytes. */
    @ModelAttribute("maxAudioBytes")
    public long maxAudioBytes() {
        return storage.getMaxAudioBytes();
    }

    @ModelAttribute("maxImageBytes")
    public long maxImageBytes() {
        return storage.getMaxImageBytes();
    }

    @ModelAttribute("maxAudioLabel")
    public String maxAudioLabel() {
        return FileStorageService.megabytes(storage.getMaxAudioBytes());
    }

    @ModelAttribute("maxImageLabel")
    public String maxImageLabel() {
        return FileStorageService.megabytes(storage.getMaxImageBytes());
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @ModelAttribute("unseenCommentCount")
    public Long unseenCommentCount(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null || !user.hasRole(Role.STORYTELLER)) {
            return 0L;
        }
        return commentService.countUnseenForStoryteller(user);
    }

    @ModelAttribute("defaultDescription")
    public String defaultDescription() {
        return DEFAULT_DESCRIPTION;
    }

    /** Path of the current page, used to highlight the active item in the mobile bottom nav. */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null ? "/" : path;
    }

    /**
     * Default robots directive. Private / thin pages get noindex.
     * Controllers may override with {@code pageRobots}.
     */
    @ModelAttribute("pageRobots")
    public String pageRobots(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return "index, follow";
        }
        if (path.startsWith("/admin")
                || path.startsWith("/dashboard")
                || path.startsWith("/storyteller")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/forgot-password")
                || path.equals("/feedback")
                || path.equals("/error")) {
            return "noindex, nofollow";
        }
        return "index, follow";
    }

    /**
     * Preferred absolute URL for this page (path only, no query string).
     * Uses {@code app.public-base-url} when set so secondary domains never win SEO.
     * Controllers may override with {@code pageCanonical}.
     */
    @ModelAttribute("canonicalUrl")
    public String canonicalUrl(HttpServletRequest request) {
        return siteUrl.canonical(request);
    }

    @ModelAttribute("publicBaseUrl")
    public String publicBaseUrl(HttpServletRequest request) {
        return siteUrl.base(request);
    }
}
