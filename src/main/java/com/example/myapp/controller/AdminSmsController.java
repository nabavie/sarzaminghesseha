package com.example.myapp.controller;

import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.SmsIrClient;
import com.example.myapp.util.MobileNumbers;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin/sms")
public class AdminSmsController {

    private final UserRepository userRepository;
    private final SmsIrClient smsIrClient;

    public AdminSmsController(UserRepository userRepository, SmsIrClient smsIrClient) {
        this.userRepository = userRepository;
        this.smsIrClient = smsIrClient;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("mobileUserCount", userRepository.countByEnabledTrueAndMobileIsNotNull());
        return "admin/sms";
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> search(@RequestParam(defaultValue = "") String q) {
        String query = MobileNumbers.toAsciiDigits(q == null ? "" : q.trim());
        return userRepository.searchEnabledWithMobile(query, PageRequest.of(0, 20)).stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "displayName", u.getDisplayName(),
                        "username", u.getUsername(),
                        "mobile", u.getMobile() == null ? "" : u.getMobile()))
                .toList();
    }

    @PostMapping
    public String send(@RequestParam String messageText,
                       @RequestParam(required = false) Boolean allUsers,
                       @RequestParam(required = false) List<Long> userIds,
                       RedirectAttributes redirect) {
        String text = messageText == null ? "" : messageText.trim();
        if (text.isEmpty()) {
            redirect.addFlashAttribute("error", "متن پیامک را بنویسید");
            return "redirect:/admin/sms";
        }
        List<String> mobiles;
        try {
            if (Boolean.TRUE.equals(allUsers)) {
                mobiles = userRepository.findByEnabledTrueAndMobileIsNotNull().stream()
                        .map(User::getMobile)
                        .filter(m -> m != null && !m.isBlank())
                        .distinct()
                        .toList();
            } else {
                Set<String> unique = new LinkedHashSet<>();
                if (userIds != null) {
                    for (Long id : userIds) {
                        userRepository.findById(id)
                                .filter(User::isEnabled)
                                .map(User::getMobile)
                                .filter(m -> m != null && !m.isBlank())
                                .ifPresent(unique::add);
                    }
                }
                mobiles = new ArrayList<>(unique);
            }
            smsIrClient.sendBulk(text, mobiles);
            redirect.addFlashAttribute("success",
                    "پیامک برای " + mobiles.size() + " شماره ارسال شد");
        } catch (SmsIrClient.SmsException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sms";
    }
}
