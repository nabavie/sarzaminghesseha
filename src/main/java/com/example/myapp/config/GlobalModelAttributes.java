package com.example.myapp.config;

import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.CommentService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    public static final String DEFAULT_DESCRIPTION =
            "سرزمین قصه‌ها — قصه صوتی، قصه شب، داستان کودکانه و نوجوانانه، و قصه گویی. "
                    + "قصه‌های صوتی برای کودکان، نوجوانان و همهٔ دوستداران قصه.";

    public static final String DEFAULT_KEYWORDS =
            "قصه, قصه گویی, قصه شب, داستان کودکانه, داستان نوجوانانه, قصه صوتی, داستان صوتی, "
                    + "قصه شب کودکان, قصه شب نوجوانان, قصه های شب, قصه شب صوتی, قصه شب برای خواب";

    private final UserRepository userRepository;
    private final CommentService commentService;

    public GlobalModelAttributes(UserRepository userRepository, CommentService commentService) {
        this.userRepository = userRepository;
        this.commentService = commentService;
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

    @ModelAttribute("defaultKeywords")
    public String defaultKeywords() {
        return DEFAULT_KEYWORDS;
    }
}
