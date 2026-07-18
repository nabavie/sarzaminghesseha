package com.example.myapp.controller;

import com.example.myapp.model.TaleStatus;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.SiteFeedbackService;
import com.example.myapp.service.StorytellerRequestService;
import com.example.myapp.service.TaleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TaleService taleService;
    private final UserRepository userRepository;
    private final StorytellerRequestService requestService;
    private final SiteFeedbackService feedbackService;

    public AdminController(TaleService taleService,
                           UserRepository userRepository,
                           StorytellerRequestService requestService,
                           SiteFeedbackService feedbackService) {
        this.taleService = taleService;
        this.userRepository = userRepository;
        this.requestService = requestService;
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pendingCount", taleService.countByStatus(TaleStatus.PENDING));
        model.addAttribute("approvedCount", taleService.countByStatus(TaleStatus.APPROVED));
        model.addAttribute("rejectedCount", taleService.countByStatus(TaleStatus.REJECTED));
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("pendingRequestCount", requestService.pendingCount());
        model.addAttribute("unseenFeedbackCount", feedbackService.countUnseen());
        return "admin/index";
    }
}
