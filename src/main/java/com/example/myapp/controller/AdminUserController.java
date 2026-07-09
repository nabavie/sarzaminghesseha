package com.example.myapp.controller;

import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("usersPage",
                userRepository.findAll(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "admin/users";
    }

    /** Disable (or re-enable) a user's account; disabled users cannot log in. */
    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int page,
                                Authentication authentication,
                                RedirectAttributes redirect) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (user.getUsername().equals(authentication.getName())) {
            redirect.addFlashAttribute("error", "نمی‌توانید حساب خودتان را غیرفعال کنید");
        } else {
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            redirect.addFlashAttribute("success", user.isEnabled()
                    ? "حساب «" + user.getDisplayName() + "» فعال شد و می‌تواند وارد شود"
                    : "حساب «" + user.getDisplayName() + "» غیرفعال شد و دیگر نمی‌تواند وارد شود");
        }
        return "redirect:/admin/users?page=" + page;
    }
}
