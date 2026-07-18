package com.example.myapp.controller;

import com.example.myapp.service.SiteFeedbackService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    private final SiteFeedbackService feedbackService;

    public AdminFeedbackController(SiteFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("feedbackPage", feedbackService.findAll(PageRequest.of(page, 20)));
        model.addAttribute("unseenCount", feedbackService.countUnseen());
        return "admin/feedback";
    }

    @PostMapping("/{id}/seen")
    public String markSeen(@PathVariable Long id,
                           @RequestParam(defaultValue = "0") int page,
                           RedirectAttributes redirect) {
        feedbackService.markSeen(id);
        redirect.addFlashAttribute("success", "پیام به‌عنوان خوانده‌شده علامت خورد");
        return "redirect:/admin/feedback?page=" + page;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes redirect) {
        feedbackService.delete(id);
        redirect.addFlashAttribute("success", "پیام حذف شد");
        return "redirect:/admin/feedback?page=" + page;
    }
}
