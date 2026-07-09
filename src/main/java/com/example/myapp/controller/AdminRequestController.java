package com.example.myapp.controller;

import com.example.myapp.model.StorytellerRequest;
import com.example.myapp.service.StorytellerRequestService;
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
@RequestMapping("/admin/requests")
public class AdminRequestController {

    private final StorytellerRequestService requestService;

    public AdminRequestController(StorytellerRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("requests", requestService.all());
        return "admin/requests";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String note,
                          RedirectAttributes redirect) {
        requestService.approve(request(id), note);
        redirect.addFlashAttribute("success", "درخواست پذیرفته شد؛ کاربر با ورود دوباره می‌تواند قصه بگوید.");
        return "redirect:/admin/requests";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirect) {
        requestService.reject(request(id), note);
        redirect.addFlashAttribute("success", "درخواست رد شد.");
        return "redirect:/admin/requests";
    }

    private StorytellerRequest request(Long id) {
        return requestService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
