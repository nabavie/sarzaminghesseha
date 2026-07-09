package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.service.FileStorageService;
import com.example.myapp.service.MediaTokenService;
import com.example.myapp.service.TaleService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.util.Locale;

@Controller
public class MediaController {

    private final TaleService taleService;
    private final FileStorageService storage;
    private final MediaTokenService mediaToken;

    public MediaController(TaleService taleService, FileStorageService storage, MediaTokenService mediaToken) {
        this.taleService = taleService;
        this.storage = storage;
        this.mediaToken = mediaToken;
    }

    /**
     * Streams a tale's audio. Returning a Resource lets Spring MVC honour
     * HTTP Range requests automatically, so the browser player can seek.
     * Requires a short-lived signed token (minted into the page next to the
     * player) so the audio has no stable, shareable download URL.
     */
    @GetMapping("/tales/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Long id,
                                          @RequestParam(name = "t", required = false) String token,
                                          Authentication authentication) {
        Tale tale = taleService.findById(id).orElse(null);
        if (tale == null || !canListen(tale, authentication) || !mediaToken.isValid(id, token)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = storage.load(FileStorageService.AUDIO, tale.getAudioPath());
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (tale.getAudioContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(tale.getAudioContentType());
            } catch (Exception ignored) {
                // keep octet-stream fallback
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.noStore())
                .body(resource);
    }

    @GetMapping("/media/covers/{filename:.+}")
    public ResponseEntity<Resource> cover(@PathVariable String filename) {
        return image(FileStorageService.COVERS, filename);
    }

    @GetMapping("/media/avatars/{filename:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String filename) {
        return image(FileStorageService.AVATARS, filename);
    }

    private ResponseEntity<Resource> image(String subdir, String filename) {
        Resource resource = storage.load(subdir, filename);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(imageType(filename))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(resource);
    }

    private MediaType imageType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
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
