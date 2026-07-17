package com.example.myapp.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds absolute public URLs. When {@code app.public-base-url} is set (production),
 * all SEO URLs use that host so a secondary domain never becomes the indexed one.
 */
@Component("siteUrl")
public class SiteUrl {

    private final String configuredBase;

    public SiteUrl(@Value("${app.public-base-url:}") String configuredBase) {
        this.configuredBase = configuredBase == null ? "" : configuredBase.strip().replaceAll("/+$", "");
    }

    public String base(HttpServletRequest request) {
        if (!configuredBase.isBlank()) {
            return configuredBase;
        }
        return requestBase(request);
    }

    /** Absolute URL for the current path (no query string). */
    public String canonical(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        return base(request) + path;
    }

    private static String requestBase(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return proto + "://" + host;
    }
}
