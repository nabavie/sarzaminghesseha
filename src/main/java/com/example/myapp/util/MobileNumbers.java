package com.example.myapp.util;

import java.util.regex.Pattern;

/**
 * Optional Iranian mobile normalization: blank → {@code null}; valid forms → {@code 09#########}.
 */
public final class MobileNumbers {

    private static final Pattern LOCAL = Pattern.compile("^09\\d{9}$");
    private static final Pattern PLUS = Pattern.compile("^\\+989\\d{9}$");
    private static final Pattern INTL = Pattern.compile("^989\\d{9}$");

    private MobileNumbers() {
    }

    /**
     * @return {@code null} when blank; normalized {@code 09…} when valid
     * @throws IllegalArgumentException when non-blank but not a valid Iranian mobile
     */
    public static String normalizeOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = toAsciiDigits(raw.trim()).replaceAll("[\\s\\-()]", "");
        if (s.isEmpty()) {
            return null;
        }
        if (LOCAL.matcher(s).matches()) {
            return s;
        }
        if (PLUS.matcher(s).matches()) {
            return "0" + s.substring(3);
        }
        if (INTL.matcher(s).matches()) {
            return "0" + s.substring(2);
        }
        throw new IllegalArgumentException(
                "شماره موبایل معتبر نیست؛ مثلاً ۰۹۱۲۳۴۵۶۷۸۹ را وارد کنید یا خالی بگذارید");
    }

    private static String toAsciiDigits(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') {
                sb.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + (c - '\u0660')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
