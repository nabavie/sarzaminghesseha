package com.example.myapp.controller;

import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/storytellers")
public class StorytellerPublicController {

    private static final int PAGE_SIZE = 12;

    private final UserService userService;
    private final TaleService taleService;

    public StorytellerPublicController(UserService userService, TaleService taleService) {
        this.userService = userService;
        this.taleService = taleService;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        User storyteller = userService.findById(id)
                .filter(u -> u.hasRole(Role.STORYTELLER))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        var talesPage = taleService.findApprovedByStoryteller(storyteller, Math.max(page, 0), PAGE_SIZE);
        int lastPage = Math.max(talesPage.getTotalPages() - 1, 0);

        model.addAttribute("storyteller", storyteller);
        model.addAttribute("talesPage", talesPage);
        model.addAttribute("taleCount", taleService.countApprovedByStoryteller(storyteller));
        model.addAttribute("pageWindowStart", Math.max(0, talesPage.getNumber() - 2));
        model.addAttribute("pageWindowEnd", Math.min(lastPage, talesPage.getNumber() + 2));
        model.addAttribute("pageDescription",
                "قصه‌های صوتی «" + storyteller.getDisplayName() + "» — قصه شب و داستان برای کودکان و نوجوانان در سرزمین قصه‌ها");
        if (storyteller.getAvatarPath() != null && !storyteller.getAvatarPath().isBlank()) {
            model.addAttribute("pageImage", "/media/avatars/" + storyteller.getAvatarPath());
        }
        return "storytellers/profile";
    }
}
