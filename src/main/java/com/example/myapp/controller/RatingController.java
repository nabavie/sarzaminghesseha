package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.RatingService;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class RatingController {

    private final RatingService ratingService;
    private final TaleService taleService;
    private final UserService userService;

    public RatingController(RatingService ratingService, TaleService taleService, UserService userService) {
        this.ratingService = ratingService;
        this.taleService = taleService;
        this.userService = userService;
    }

    @PostMapping("/tales/{id}/rating")
    public String rate(@PathVariable Long id, @RequestParam int stars, Principal principal) {
        Tale tale = taleService.findById(id)
                .filter(t -> t.getStatus() == TaleStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        ratingService.rate(user, tale, stars);
        return "redirect:/tales/" + id;
    }
}
