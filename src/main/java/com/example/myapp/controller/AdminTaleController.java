package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.service.TaleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/admin/tales")
public class AdminTaleController {

    private final TaleService taleService;

    public AdminTaleController(TaleService taleService) {
        this.taleService = taleService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") TaleStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("talesPage", taleService.findByStatus(status, PageRequest.of(page, 15)));
        model.addAttribute("status", status);
        model.addAttribute("pendingCount", taleService.countByStatus(TaleStatus.PENDING));
        model.addAttribute("approvedCount", taleService.countByStatus(TaleStatus.APPROVED));
        model.addAttribute("rejectedCount", taleService.countByStatus(TaleStatus.REJECTED));
        return "admin/tales";
    }

    @GetMapping("/{id}")
    public String review(@PathVariable Long id, Model model) {
        model.addAttribute("tale", tale(id));
        return "admin/tale-review";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        taleService.approve(tale(id), note);
        return "redirect:/admin/tales?approved";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note) {
        taleService.reject(tale(id), note);
        return "redirect:/admin/tales?rejected";
    }

    private Tale tale(Long id) {
        return taleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
