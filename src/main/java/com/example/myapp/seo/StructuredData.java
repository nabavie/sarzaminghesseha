package com.example.myapp.seo;

import com.example.myapp.model.Category;
import com.example.myapp.model.Tale;
import com.example.myapp.model.User;
import com.example.myapp.util.JsonLd;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * Builds Schema.org JSON-LD graphs for public pages.
 * Note: this app serves audio tales (not video blocks); AudioObject is the correct type.
 */
public final class StructuredData {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private StructuredData() {
    }

    public static String website(String baseUrl) {
        String logo = baseUrl + "/img/logo-hero.png";
        return """
                {
                  "@context": "https://schema.org",
                  "@graph": [
                    {
                      "@type": "WebSite",
                      "@id": "%s/#website",
                      "url": "%s/",
                      "name": "سرزمین قصه‌ها",
                      "description": "قصه صوتی، قصه شب و داستان برای کودکان و نوجوانان",
                      "inLanguage": "fa-IR",
                      "publisher": { "@id": "%s/#organization" },
                      "potentialAction": {
                        "@type": "SearchAction",
                        "target": {
                          "@type": "EntryPoint",
                          "urlTemplate": "%s/tales?q={search_term_string}"
                        },
                        "query-input": "required name=search_term_string"
                      }
                    },
                    {
                      "@type": "Organization",
                      "@id": "%s/#organization",
                      "name": "سرزمین قصه‌ها",
                      "url": "%s/",
                      "logo": {
                        "@type": "ImageObject",
                        "url": "%s"
                      }
                    }
                  ]
                }
                """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, logo);
    }

    public static String tale(String baseUrl, Tale tale, String description,
                              double avgRating, long ratingCount) {
        String pageUrl = baseUrl + "/tales/" + tale.getId();
        String image = tale.getCoverPath() != null && !tale.getCoverPath().isBlank()
                ? baseUrl + "/media/covers/" + tale.getCoverPath()
                : baseUrl + "/img/logo.png";
        String duration = JsonLd.durationIso(tale.getDurationSeconds());
        Instant published = tale.getApprovedAt() != null ? tale.getApprovedAt() : tale.getCreatedAt();
        String datePublished = published != null
                ? ISO.format(published.atOffset(ZoneOffset.UTC)) : null;

        StringJoiner genres = new StringJoiner(", ");
        if (tale.getCategories() != null) {
            for (Category cat : tale.getCategories()) {
                if (cat != null && cat.getName() != null) {
                    genres.add(cat.getName());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"@context\": \"https://schema.org\",\n");
        sb.append("  \"@graph\": [\n");

        // BreadcrumbList
        sb.append("    {\n");
        sb.append("      \"@type\": \"BreadcrumbList\",\n");
        sb.append("      \"itemListElement\": [\n");
        sb.append("        {\n");
        sb.append("          \"@type\": \"ListItem\",\n");
        sb.append("          \"position\": 1,\n");
        sb.append("          \"name\": \"خانه\",\n");
        sb.append("          \"item\": ").append(JsonLd.quoted(baseUrl + "/")).append("\n");
        sb.append("        },\n");
        sb.append("        {\n");
        sb.append("          \"@type\": \"ListItem\",\n");
        sb.append("          \"position\": 2,\n");
        sb.append("          \"name\": \"قصه‌ها\",\n");
        sb.append("          \"item\": ").append(JsonLd.quoted(baseUrl + "/tales")).append("\n");
        sb.append("        },\n");
        sb.append("        {\n");
        sb.append("          \"@type\": \"ListItem\",\n");
        sb.append("          \"position\": 3,\n");
        sb.append("          \"name\": ").append(JsonLd.quoted(tale.getTitle())).append(",\n");
        sb.append("          \"item\": ").append(JsonLd.quoted(pageUrl)).append("\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    },\n");

        // AudioObject (spoken tale — not VideoObject; this product has no video blocks)
        sb.append("    {\n");
        sb.append("      \"@type\": \"AudioObject\",\n");
        sb.append("      \"@id\": ").append(JsonLd.quoted(pageUrl + "#audio")).append(",\n");
        sb.append("      \"name\": ").append(JsonLd.quoted(tale.getTitle())).append(",\n");
        sb.append("      \"description\": ").append(JsonLd.quoted(description)).append(",\n");
        sb.append("      \"url\": ").append(JsonLd.quoted(pageUrl)).append(",\n");
        sb.append("      \"image\": ").append(JsonLd.quoted(image)).append(",\n");
        sb.append("      \"inLanguage\": \"fa\",\n");
        sb.append("      \"isFamilyFriendly\": true,\n");
        if (tale.getAudioContentType() != null && !tale.getAudioContentType().isBlank()) {
            sb.append("      \"encodingFormat\": ").append(JsonLd.quoted(tale.getAudioContentType())).append(",\n");
        }
        if (duration != null) {
            sb.append("      \"duration\": ").append(JsonLd.quoted(duration)).append(",\n");
        }
        if (datePublished != null) {
            sb.append("      \"datePublished\": ").append(JsonLd.quoted(datePublished)).append(",\n");
        }
        if (genres.length() > 0) {
            sb.append("      \"genre\": ").append(JsonLd.quoted(genres.toString())).append(",\n");
        }
        User storyteller = tale.getStoryteller();
        if (storyteller != null) {
            sb.append("      \"author\": {\n");
            sb.append("        \"@type\": \"Person\",\n");
            sb.append("        \"name\": ").append(JsonLd.quoted(storyteller.getDisplayName())).append(",\n");
            sb.append("        \"url\": ").append(JsonLd.quoted(baseUrl + "/storytellers/" + storyteller.getId())).append("\n");
            sb.append("      },\n");
        }
        sb.append("      \"publisher\": {\n");
        sb.append("        \"@type\": \"Organization\",\n");
        sb.append("        \"name\": \"سرزمین قصه‌ها\",\n");
        sb.append("        \"url\": ").append(JsonLd.quoted(baseUrl + "/")).append("\n");
        sb.append("      }");
        if (ratingCount > 0 && avgRating > 0) {
            sb.append(",\n");
            sb.append("      \"aggregateRating\": {\n");
            sb.append("        \"@type\": \"AggregateRating\",\n");
            sb.append("        \"ratingValue\": ").append(String.format(java.util.Locale.US, "%.1f", avgRating)).append(",\n");
            sb.append("        \"bestRating\": 5,\n");
            sb.append("        \"worstRating\": 1,\n");
            sb.append("        \"ratingCount\": ").append(ratingCount).append("\n");
            sb.append("      }\n");
        } else {
            sb.append("\n");
        }
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String storyteller(String baseUrl, User storyteller, long taleCount) {
        String pageUrl = baseUrl + "/storytellers/" + storyteller.getId();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"@context\": \"https://schema.org\",\n");
        sb.append("  \"@graph\": [\n");
        sb.append("    {\n");
        sb.append("      \"@type\": \"BreadcrumbList\",\n");
        sb.append("      \"itemListElement\": [\n");
        sb.append("        {\n");
        sb.append("          \"@type\": \"ListItem\",\n");
        sb.append("          \"position\": 1,\n");
        sb.append("          \"name\": \"خانه\",\n");
        sb.append("          \"item\": ").append(JsonLd.quoted(baseUrl + "/")).append("\n");
        sb.append("        },\n");
        sb.append("        {\n");
        sb.append("          \"@type\": \"ListItem\",\n");
        sb.append("          \"position\": 2,\n");
        sb.append("          \"name\": ").append(JsonLd.quoted(storyteller.getDisplayName())).append(",\n");
        sb.append("          \"item\": ").append(JsonLd.quoted(pageUrl)).append("\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"@type\": \"Person\",\n");
        sb.append("      \"@id\": ").append(JsonLd.quoted(pageUrl + "#person")).append(",\n");
        sb.append("      \"name\": ").append(JsonLd.quoted(storyteller.getDisplayName())).append(",\n");
        sb.append("      \"url\": ").append(JsonLd.quoted(pageUrl)).append(",\n");
        sb.append("      \"jobTitle\": \"قصه‌گو\",\n");
        sb.append("      \"description\": ").append(JsonLd.quoted(
                "قصه‌های صوتی «" + storyteller.getDisplayName() + "» — "
                        + taleCount + " قصه در سرزمین قصه‌ها")).append(",\n");
        if (storyteller.getAvatarPath() != null && !storyteller.getAvatarPath().isBlank()) {
            sb.append("      \"image\": ").append(JsonLd.quoted(
                    baseUrl + "/media/avatars/" + storyteller.getAvatarPath())).append(",\n");
        }
        sb.append("      \"knowsAbout\": [\"قصه صوتی\", \"قصه شب\", \"قصه گویی\"],\n");
        sb.append("      \"worksFor\": {\n");
        sb.append("        \"@type\": \"Organization\",\n");
        sb.append("        \"name\": \"سرزمین قصه‌ها\",\n");
        sb.append("        \"url\": ").append(JsonLd.quoted(baseUrl + "/")).append("\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String talesCollection(String baseUrl) {
        return """
                {
                  "@context": "https://schema.org",
                  "@type": "CollectionPage",
                  "name": "همهٔ قصه‌ها",
                  "description": "فهرست قصه صوتی، قصه شب و داستان برای کودکان و نوجوانان",
                  "url": "%s/tales",
                  "isPartOf": {
                    "@type": "WebSite",
                    "name": "سرزمین قصه‌ها",
                    "url": "%s/"
                  },
                  "inLanguage": "fa-IR"
                }
                """.formatted(baseUrl, baseUrl);
    }

    public static String aboutPage(String baseUrl) {
        String pageUrl = baseUrl + "/about";
        return """
                {
                  "@context": "https://schema.org",
                  "@graph": [
                    {
                      "@type": "AboutPage",
                      "@id": "%s#webpage",
                      "url": "%s",
                      "name": "دربارهٔ سرزمین قصه‌ها",
                      "description": "آشنایی با سرزمین قصه‌ها، هدف‌ها و مخاطبان وب‌سایت قصه صوتی فارسی",
                      "inLanguage": "fa-IR",
                      "isPartOf": { "@id": "%s/#website" },
                      "about": { "@id": "%s/#organization" }
                    },
                    {
                      "@type": "Organization",
                      "@id": "%s/#organization",
                      "name": "سرزمین قصه‌ها",
                      "url": "%s/",
                      "description": "پلتفرم قصه صوتی و قصه شب برای کودکان، نوجوانان و خانواده‌ها",
                      "logo": {
                        "@type": "ImageObject",
                        "url": "%s/img/logo-hero.png"
                      }
                    }
                  ]
                }
                """.formatted(pageUrl, pageUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
    }

    public static String faqPage(String baseUrl, java.util.List<FaqItem> faqs) {
        String pageUrl = baseUrl + "/faq";
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"@context\": \"https://schema.org\",\n");
        sb.append("  \"@type\": \"FAQPage\",\n");
        sb.append("  \"url\": ").append(JsonLd.quoted(pageUrl)).append(",\n");
        sb.append("  \"name\": ").append(JsonLd.quoted("سؤالات متداول — سرزمین قصه‌ها")).append(",\n");
        sb.append("  \"inLanguage\": \"fa-IR\",\n");
        sb.append("  \"mainEntity\": [\n");
        for (int i = 0; i < faqs.size(); i++) {
            FaqItem item = faqs.get(i);
            sb.append("    {\n");
            sb.append("      \"@type\": \"Question\",\n");
            sb.append("      \"name\": ").append(JsonLd.quoted(item.question())).append(",\n");
            sb.append("      \"acceptedAnswer\": {\n");
            sb.append("        \"@type\": \"Answer\",\n");
            sb.append("        \"text\": ").append(JsonLd.quoted(item.answer())).append("\n");
            sb.append("      }\n");
            sb.append("    }");
            if (i < faqs.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
