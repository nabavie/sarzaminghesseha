package com.example.myapp.controller;

import com.example.myapp.model.Comment;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.CommentService;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class CommentController {

    private final CommentService commentService;
    private final TaleService taleService;
    private final UserService userService;

    public CommentController(CommentService commentService, TaleService taleService, UserService userService) {
        this.commentService = commentService;
        this.taleService = taleService;
        this.userService = userService;
    }

    @PostMapping("/tales/{id}/comments")
    public String add(@PathVariable Long id,
                      @RequestParam String content,
                      @RequestParam(required = false) Long parentId,
                      Principal principal,
                      RedirectAttributes redirect) {
        Tale tale = taleService.findById(id)
                .filter(t -> t.getStatus() == TaleStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            redirect.addFlashAttribute("commentError", "نظر نمی‌تواند خالی باشد");
            return "redirect:/tales/" + id + "#comments";
        }
        if (trimmed.length() > 1000) {
            redirect.addFlashAttribute("commentError", "نظر خیلی طولانی است (بیشترین اندازه ۱۰۰۰ حرف)");
            return "redirect:/tales/" + id + "#comments";
        }

        Comment parent = null;
        if (parentId != null) {
            parent = commentService.findById(parentId)
                    .filter(c -> c.getTale().getId().equals(tale.getId()))
                    .orElse(null);
        }
        commentService.add(tale, user, parent, trimmed);
        return "redirect:/tales/" + id + "#comments";
    }
}
