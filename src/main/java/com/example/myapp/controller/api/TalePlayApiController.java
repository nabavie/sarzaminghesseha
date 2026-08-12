package com.example.myapp.controller.api;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.service.MediaTokenService;
import com.example.myapp.service.TaleService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TalePlayApiController {

    private final TaleService taleService;
    private final MediaTokenService mediaToken;

    public TalePlayApiController(TaleService taleService, MediaTokenService mediaToken) {
        this.taleService = taleService;
        this.mediaToken = mediaToken;
    }

    public record PlayResponse(long id, String title, String audioUrl, String coverUrl,
                               Long nextId, String nextTitle) {
    }

    /**
     * Enough data to continue playback on the same {@code <audio>} element
     * without a full page load — browsers block autoplay after navigation,
     * especially in a background tab.
     */
    @GetMapping("/api/tales/{id}/play")
    @Transactional(readOnly = true)
    public ResponseEntity<PlayResponse> play(@PathVariable Long id, Authentication authentication) {
        Tale tale = taleService.findById(id).orElse(null);
        if (tale == null || !canListen(tale, authentication)) {
            return ResponseEntity.notFound().build();
        }
        Tale next = taleService.findNextToPlay(tale).orElse(null);
        String audioUrl = "/tales/" + tale.getId() + "/audio?t=" + mediaToken.issue(tale.getId());
        String coverUrl = tale.getCoverPath() == null ? null : "/media/covers/" + tale.getCoverPath();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new PlayResponse(
                        tale.getId(),
                        tale.getTitle(),
                        audioUrl,
                        coverUrl,
                        next == null ? null : next.getId(),
                        next == null ? null : next.getTitle()));
    }

    private boolean canListen(Tale tale, Authentication authentication) {
        if (tale.getStatus() == TaleStatus.APPROVED) {
            return true;
        }
        if (authentication == null) {
            return false;
        }
        boolean admin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean owner = tale.getStoryteller().getUsername().equals(authentication.getName());
        return admin || owner;
    }
}
