package com.example.myapp.controller;

import com.example.myapp.model.Category;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.service.CategoryService;
import com.example.myapp.service.TaleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/admin/tales")
public class AdminTaleController {

    private final TaleService taleService;
    private final CategoryService categoryService;

    public AdminTaleController(TaleService taleService, CategoryService categoryService) {
        this.taleService = taleService;
        this.categoryService = categoryService;
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
        Tale tale = tale(id);
        model.addAttribute("tale", tale);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryIds",
                tale.getCategories().stream().map(Category::getId).collect(Collectors.toList()));
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

    @PostMapping("/{id}/edit-content")
    public String editContent(@PathVariable Long id,
                              @RequestParam(required = false) String title,
                              @RequestParam String description,
                              @RequestParam(required = false) String seoDescription,
                              @RequestParam(required = false) List<Long> categoryIds,
                              @RequestParam(required = false) MultipartFile cover,
                              RedirectAttributes redirect) {
        try {
            taleService.adminUpdateContent(tale(id), title, description, seoDescription,
                    resolveCategories(categoryIds), cover);
            redirect.addFlashAttribute("success", "نام، دسته، توضیح، متن سئو و تصویر قصه به‌روز شد");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tales/" + id;
    }

    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null) {
            return Set.of();
        }
        return categoryIds.stream()
                .map(categoryService::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Tale tale(Long id) {
        return taleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
