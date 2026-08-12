package com.example.myapp.util;

/**
 * Helpers for building safe JSON-LD strings for SEO structured data.
 */
public final class JsonLd {

    private JsonLd() {
    }

    /** Escape a string for use inside a JSON string value. */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> {
                    // Prevent </script> breakout inside JSON-LD script tags
                    if (i + 1 < value.length() && value.charAt(i + 1) == '/') {
                        sb.append("<\\/");
                        i++;
                    } else {
                        sb.append(c);
                    }
                }
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** ISO-8601 duration (e.g. PT5M30S) for Schema.org AudioObject.duration. */
    public static String durationIso(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder("PT");
        if (h > 0) {
            sb.append(h).append('H');
        }
        if (m > 0) {
            sb.append(m).append('M');
        }
        if (s > 0 || (h == 0 && m == 0)) {
            sb.append(s).append('S');
        }
        return sb.toString();
    }

    public static String quoted(String value) {
        return "\"" + escape(value) + "\"";
    }
}
