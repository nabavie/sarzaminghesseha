package com.example.myapp.controller;

import com.example.myapp.dto.SiteFeedbackForm;
import com.example.myapp.service.FeedbackRateLimitService;
import com.example.myapp.service.SiteFeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    private final SiteFeedbackService feedbackService;
    private final FeedbackRateLimitService rateLimit;

    public FeedbackController(SiteFeedbackService feedbackService, FeedbackRateLimitService rateLimit) {
        this.feedbackService = feedbackService;
        this.rateLimit = rateLimit;
    }

    @GetMapping
    public String form(@ModelAttribute("form") SiteFeedbackForm form) {
        return "feedback/form";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") SiteFeedbackForm form,
                         BindingResult result,
                         HttpServletRequest request,
                         RedirectAttributes redirect) {
        String ip = request.getRemoteAddr();
        if (!rateLimit.isAllowed(ip)) {
            result.reject("rate", "لطفاً کمی صبر کنید و دوباره تلاش کنید");
            return "feedback/form";
        }
        if (result.hasErrors()) {
            return "feedback/form";
        }
        feedbackService.submit(form);
        rateLimit.record(ip);
        redirect.addFlashAttribute("success", "پیام شما ثبت شد. سپاس از همراهی‌تان!");
        return "redirect:/feedback";
    }
}
