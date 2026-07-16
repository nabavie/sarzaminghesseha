package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.service.TaleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class SeoController {

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final TaleService taleService;

    public SeoController(TaleService taleService) {
        this.taleService = taleService;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String base = baseUrl(request);
        return """
                User-agent: *
                Allow: /
                Disallow: /admin/
                Disallow: /dashboard/
                Disallow: /storyteller/
                Disallow: /login
                Disallow: /register
                Disallow: /forgot-password

                Sitemap: %s/sitemap.xml
                """.formatted(base);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String base = baseUrl(request);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        url(sb, base + "/", Instant.now(), "1.0", "daily");
        url(sb, base + "/tales", Instant.now(), "0.9", "daily");

        List<Tale> tales = taleService.findRecentApproved(500);
        for (Tale tale : tales) {
            Instant lastmod = tale.getApprovedAt() != null ? tale.getApprovedAt()
                    : (tale.getCreatedAt() != null ? tale.getCreatedAt() : Instant.now());
            url(sb, base + "/tales/" + tale.getId(), lastmod, "0.8", "weekly");
            if (tale.getStoryteller() != null) {
                url(sb, base + "/storytellers/" + tale.getStoryteller().getId(), lastmod, "0.7", "weekly");
            }
        }
        sb.append("</urlset>");
        return sb.toString();
    }

    private static void url(StringBuilder sb, String loc, Instant lastmod, String priority, String changefreq) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escape(loc)).append("</loc>\n");
        sb.append("    <lastmod>").append(W3C.format(lastmod.atOffset(ZoneOffset.UTC))).append("</lastmod>\n");
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }

    private static String baseUrl(HttpServletRequest request) {
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

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
