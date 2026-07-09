package com.example.myapp.controller.api;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.ProgressService;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ProgressApiController {

    private final ProgressService progressService;
    private final TaleService taleService;
    private final UserService userService;

    public ProgressApiController(ProgressService progressService,
                                 TaleService taleService,
                                 UserService userService) {
        this.progressService = progressService;
        this.taleService = taleService;
        this.userService = userService;
    }

    public record ProgressRequest(Long taleId, Integer seconds, Integer duration, Boolean finished) {
    }

    @PostMapping("/api/progress")
    public ResponseEntity<Void> record(@RequestBody ProgressRequest request, Principal principal) {
        if (request.taleId() == null || request.seconds() == null) {
            return ResponseEntity.badRequest().build();
        }
        Tale tale = taleService.findById(request.taleId())
                .filter(t -> t.getStatus() == TaleStatus.APPROVED)
                .orElse(null);
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (tale == null || user == null) {
            return ResponseEntity.notFound().build();
        }
        progressService.record(user, tale, request.seconds(), request.duration(),
                Boolean.TRUE.equals(request.finished()));

        // opportunistically remember the tale's duration for progress bars
        if (tale.getDurationSeconds() == null && request.duration() != null && request.duration() > 0) {
            tale.setDurationSeconds(request.duration());
            taleService.save(tale);
        }
        return ResponseEntity.ok().build();
    }
}
