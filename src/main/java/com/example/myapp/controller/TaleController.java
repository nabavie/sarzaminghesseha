package com.example.myapp.controller;

import com.example.myapp.model.ListeningProgress;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.CategoryService;
import com.example.myapp.service.CommentService;
import com.example.myapp.service.FavoriteService;
import com.example.myapp.service.ProgressService;
import com.example.myapp.service.RatingService;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class TaleController {

    private static final int PAGE_SIZE = 12;

    private final TaleService taleService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final RatingService ratingService;
    private final FavoriteService favoriteService;
    private final CommentService commentService;
    private final ProgressService progressService;

    public TaleController(TaleService taleService,
                          CategoryService categoryService,
                          UserService userService,
                          RatingService ratingService,
                          FavoriteService favoriteService,
                          CommentService commentService,
                          ProgressService progressService) {
        this.taleService = taleService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.ratingService = ratingService;
        this.favoriteService = favoriteService;
        this.commentService = commentService;
        this.progressService = progressService;
    }

    @GetMapping("/tales")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long category,
                       @RequestParam(required = false) Long storyteller,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String query = q == null ? "" : q.trim();
        var talesPage = taleService.searchApproved(query, category, storyteller, Math.max(page, 0), PAGE_SIZE);
        model.addAttribute("talesPage", talesPage);
        int lastPage = Math.max(talesPage.getTotalPages() - 1, 0);
        model.addAttribute("pageWindowStart", Math.max(0, talesPage.getNumber() - 2));
        model.addAttribute("pageWindowEnd", Math.min(lastPage, talesPage.getNumber() + 2));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("q", query);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStorytellerId", storyteller);

        List<User> matchingStorytellers = List.of();
        if (!query.isEmpty() && storyteller == null) {
            matchingStorytellers = userService.searchStorytellers(query);
            if (matchingStorytellers.size() > 8) {
                matchingStorytellers = matchingStorytellers.subList(0, 8);
            }
        }
        model.addAttribute("matchingStorytellers", matchingStorytellers);

        User selectedStoryteller = null;
        if (storyteller != null) {
            selectedStoryteller = userService.findById(storyteller).orElse(null);
        }
        model.addAttribute("selectedStoryteller", selectedStoryteller);

        model.addAttribute("pageDescription",
                "جست‌وجوی قصه صوتی، قصه شب و داستان برای کودکان و نوجوانان در سرزمین قصه‌ها.");
        return "tales/list";
    }

    @GetMapping("/tales/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        Tale tale = taleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (tale.getStatus() != TaleStatus.APPROVED && !canPreview(tale, authentication)) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        model.addAttribute("tale", tale);
        model.addAttribute("avgRating", ratingService.average(tale));
        model.addAttribute("ratingCount", ratingService.count(tale));
        model.addAttribute("comments", commentService.topLevelForTale(tale));

        User user = authentication == null ? null
                : userService.findByUsername(authentication.getName()).orElse(null);
        model.addAttribute("isFavorite", user != null && favoriteService.isFavorite(user, tale));
        model.addAttribute("userStars", user == null ? null : ratingService.userStars(user, tale));
        model.addAttribute("resumeSeconds", user == null ? 0
                : progressService.find(user, tale).map(ListeningProgress::getSeconds).orElse(0));

        String desc = tale.getDescription() == null ? "" : tale.getDescription().trim();
        if (desc.length() > 160) {
            desc = desc.substring(0, 157) + "…";
        }
        if (desc.isEmpty()) {
            desc = "قصه صوتی «" + tale.getTitle() + "» — قصه شب و داستان برای کودکان و نوجوانان در سرزمین قصه‌ها";
        }
        model.addAttribute("pageDescription", desc);
        model.addAttribute("pageImage", tale.getCoverPath() != null
                ? "/media/covers/" + tale.getCoverPath() : "/img/logo.png");
        return "tales/detail";
    }

    private boolean canPreview(Tale tale, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        boolean admin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean owner = tale.getStoryteller().getUsername().equals(authentication.getName());
        return admin || owner;
    }
}
