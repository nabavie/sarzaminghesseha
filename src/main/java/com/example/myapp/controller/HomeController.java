package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.service.CategoryService;
import com.example.myapp.service.RatingService;
import com.example.myapp.service.TaleService;
import com.example.myapp.util.PersianDateUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final TaleService taleService;
    private final RatingService ratingService;
    private final CategoryService categoryService;
    private final PersianDateUtil persianDate;

    public HomeController(TaleService taleService,
                          RatingService ratingService,
                          CategoryService categoryService,
                          PersianDateUtil persianDate) {
        this.taleService = taleService;
        this.ratingService = ratingService;
        this.categoryService = categoryService;
        this.persianDate = persianDate;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Tale> latest = taleService
                .findByStatus(TaleStatus.APPROVED, PageRequest.of(0, 8))
                .getContent();

        // top 5 of the month, padded with the latest approved tales if needed
        List<Tale> top = new ArrayList<>(ratingService.topOfMonth(5));
        for (Tale tale : latest) {
            if (top.size() >= 5) {
                break;
            }
            if (top.stream().noneMatch(t -> t.getId().equals(tale.getId()))) {
                top.add(tale);
            }
        }

        model.addAttribute("topTales", top);
        model.addAttribute("topMonthName", persianDate.currentJalaliMonthName());
        model.addAttribute("latestTales", latest);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageDescription",
                "سرزمین قصه‌ها — قصه صوتی، قصه شب، داستان کودکانه و نوجوانانه، و قصه گویی. "
                        + "قصه‌های صوتی برای کودکان، نوجوانان و همهٔ دوستداران قصه.");
        return "home/index";
    }
}
