package com.example.myapp.config;

import com.example.myapp.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The container aborts an oversized upload while parsing the request, before any
 * controller runs, so without this the storyteller only sees a raw error page.
 * Turns it into a Persian message that names the actual limits.
 */
@ControllerAdvice
public class UploadErrorAdvice {

    private final FileStorageService storage;

    public UploadErrorAdvice(FileStorageService storage) {
        this.storage = storage;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleTooLarge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = "حجم فایلی که فرستادید بیشتر از حد مجاز است؛"
                + " حداکثر اندازهٔ فایل صوتی " + FileStorageService.megabytes(storage.getMaxAudioBytes())
                + " مگابایت و اندازهٔ عکس " + FileStorageService.megabytes(storage.getMaxImageBytes())
                + " مگابایت است. لطفاً فایل سبک‌تری بفرستید.";

        if (expectsJson(request)) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
            response.flushBuffer();
            // empty ModelAndView == handled, nothing left to render
            return new ModelAndView();
        }

        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("error", message);
        var flashMapManager = RequestContextUtils.getFlashMapManager(request);
        if (flashMapManager != null) {
            flashMapManager.saveOutputFlashMap(flashMap, request, response);
        }
        return new ModelAndView("redirect:" + safeReturnPath(request));
    }

    private boolean expectsJson(HttpServletRequest request) {
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        if (uri.startsWith("/api/") || uri.endsWith("/upload-audio")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Sends the visitor back to the form they came from, but only when the referer
     * points at this site, so the error message cannot be used to bounce someone
     * to an external address.
     */
    private String safeReturnPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/";
            }
            String path = uri.getPath();
            return path == null || path.isBlank() ? "/" : path;
        } catch (URISyntaxException e) {
            return "/";
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
