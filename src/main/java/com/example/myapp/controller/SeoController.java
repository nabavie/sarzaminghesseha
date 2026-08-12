package com.example.myapp.controller;

import com.example.myapp.model.Tale;
import com.example.myapp.service.TaleService;
import com.example.myapp.util.SiteUrl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SeoController {

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int SITEMAP_TALE_LIMIT = 5000;

    private final TaleService taleService;
    private final SiteUrl siteUrl;

    public SeoController(TaleService taleService, SiteUrl siteUrl) {
        this.taleService = taleService;
        this.siteUrl = siteUrl;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String base = siteUrl.base(request);
        return """
                User-agent: *
                Allow: /
                Disallow: /admin/
                Disallow: /dashboard/
                Disallow: /storyteller/
                Disallow: /login
                Disallow: /register
                Disallow: /forgot-password
                Disallow: /feedback
                Disallow: /logout
                Disallow: /error
                Disallow: /*/audio

                Sitemap: %s/sitemap.xml
                """.formatted(base);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String base = siteUrl.base(request);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        sb.append("        xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        url(sb, base + "/", Instant.now(), "1.0", "daily", null, null);
        url(sb, base + "/tales", Instant.now(), "0.9", "daily", null, null);
        url(sb, base + "/about", Instant.now(), "0.6", "monthly", null, null);
        url(sb, base + "/faq", Instant.now(), "0.6", "monthly", null, null);

        List<Tale> tales = taleService.findRecentApproved(SITEMAP_TALE_LIMIT);
        Set<Long> storytellerIds = new HashSet<>();
        for (Tale tale : tales) {
            Instant lastmod = tale.getApprovedAt() != null ? tale.getApprovedAt()
                    : (tale.getCreatedAt() != null ? tale.getCreatedAt() : Instant.now());
            String imageLoc = null;
            String imageTitle = null;
            if (tale.getCoverPath() != null && !tale.getCoverPath().isBlank()) {
                imageLoc = base + "/media/covers/" + tale.getCoverPath();
                imageTitle = tale.getTitle();
            }
            url(sb, base + "/tales/" + tale.getId(), lastmod, "0.8", "weekly", imageLoc, imageTitle);

            if (tale.getStoryteller() != null && storytellerIds.add(tale.getStoryteller().getId())) {
                String avatarLoc = null;
                String avatarTitle = null;
                if (tale.getStoryteller().getAvatarPath() != null
                        && !tale.getStoryteller().getAvatarPath().isBlank()) {
                    avatarLoc = base + "/media/avatars/" + tale.getStoryteller().getAvatarPath();
                    avatarTitle = tale.getStoryteller().getDisplayName();
                }
                url(sb, base + "/storytellers/" + tale.getStoryteller().getId(),
                        lastmod, "0.7", "weekly", avatarLoc, avatarTitle);
            }
        }
        sb.append("</urlset>");
        return sb.toString();
    }

    private static void url(StringBuilder sb, String loc, Instant lastmod, String priority,
                            String changefreq, String imageLoc, String imageTitle) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escape(loc)).append("</loc>\n");
        sb.append("    <lastmod>").append(W3C.format(lastmod.atOffset(ZoneOffset.UTC))).append("</lastmod>\n");
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        if (imageLoc != null) {
            sb.append("    <image:image>\n");
            sb.append("      <image:loc>").append(escape(imageLoc)).append("</image:loc>\n");
            if (imageTitle != null && !imageTitle.isBlank()) {
                sb.append("      <image:title>").append(escape(imageTitle)).append("</image:title>\n");
            }
            sb.append("    </image:image>\n");
        }
        sb.append("  </url>\n");
    }

    private static String escape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
