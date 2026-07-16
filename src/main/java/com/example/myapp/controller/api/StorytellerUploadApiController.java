package com.example.myapp.controller.api;

import com.example.myapp.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Separate audio upload endpoint used by the tale form so large files can be
 * sent with an XHR progress bar instead of blocking the whole form POST
 * (which often hits gateway timeouts on slow connections).
 */
@RestController
@RequestMapping("/storyteller/tales")
public class StorytellerUploadApiController {

    public static final String PENDING_AUDIO_SESSION_KEY = "pendingAudioUploads";

    private final FileStorageService storage;

    public StorytellerUploadApiController(FileStorageService storage) {
        this.storage = storage;
    }

    @PostMapping("/upload-audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("audio") MultipartFile audio,
                                         HttpSession session) {
        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "فایل صوتی خالی است"));
        }
        try {
            String filename = storage.storeAudio(audio);
            pendingSet(session).add(filename);
            Map<String, String> body = new HashMap<>();
            body.put("filename", filename);
            body.put("contentType", audio.getContentType() == null ? "audio/mpeg" : audio.getContentType());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "آپلود ناموفق بود؛ لطفاً دوباره تلاش کنید"));
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> pendingSet(HttpSession session) {
        Object existing = session.getAttribute(PENDING_AUDIO_SESSION_KEY);
        if (existing instanceof Set<?> set) {
            return (Set<String>) set;
        }
        Set<String> created = new HashSet<>();
        session.setAttribute(PENDING_AUDIO_SESSION_KEY, created);
        return created;
    }
}
