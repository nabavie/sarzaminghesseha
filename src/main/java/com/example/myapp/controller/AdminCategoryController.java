package com.example.myapp.controller;

import com.example.myapp.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @PostMapping
    public String create(@RequestParam String name, RedirectAttributes redirect) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            redirect.addFlashAttribute("error", "نام دسته نمی‌تواند خالی باشد");
        } else if (categoryService.nameExists(trimmed)) {
            redirect.addFlashAttribute("error", "دسته‌ای با این نام از قبل وجود دارد");
        } else {
            categoryService.create(trimmed);
            redirect.addFlashAttribute("success", "دستهٔ «" + trimmed + "» اضافه شد");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        categoryService.delete(id);
        redirect.addFlashAttribute("success", "دسته حذف شد");
        return "redirect:/admin/categories";
    }
}
