package com.example.myapp.util;

import com.github.mfathi91.time.PersianDate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component("persianDate")
public class PersianDateUtil {

    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private static final String[] MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    private static final char[] PERSIAN_DIGITS = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};

    public String format(Instant instant) {
        if (instant == null) {
            return "";
        }
        LocalDate gregorian = LocalDate.ofInstant(instant, TEHRAN);
        PersianDate persian = PersianDate.fromGregorian(gregorian);
        return digits(persian.getDayOfMonth()) + " "
                + MONTHS[persian.getMonthValue() - 1] + " "
                + digits(persian.getYear());
    }

    /** Start of the current Jalali (Persian) month as an Instant, in Tehran time. */
    public Instant startOfCurrentJalaliMonth() {
        PersianDate today = PersianDate.fromGregorian(LocalDate.now(TEHRAN));
        LocalDate monthStart = PersianDate.of(today.getYear(), today.getMonthValue(), 1).toGregorian();
        return monthStart.atStartOfDay(TEHRAN).toInstant();
    }

    /** Name of the current Jalali month, e.g. "تیر". */
    public String currentJalaliMonthName() {
        PersianDate today = PersianDate.fromGregorian(LocalDate.now(TEHRAN));
        return MONTHS[today.getMonthValue() - 1];
    }

    /** Human Persian duration like "۵ دقیقه و ۳۰ ثانیه" from a seconds count. */
    public String duration(Object secondsValue) {
        long total;
        try {
            total = Long.parseLong(String.valueOf(secondsValue));
        } catch (NumberFormatException e) {
            return "";
        }
        if (total <= 0) {
            return "۰ ثانیه";
        }
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(digits(hours)).append(" ساعت");
        }
        if (minutes > 0) {
            if (sb.length() > 0) {
                sb.append(" و ");
            }
            sb.append(digits(minutes)).append(" دقیقه");
        }
        if (seconds > 0 && hours == 0) {
            if (sb.length() > 0) {
                sb.append(" و ");
            }
            sb.append(digits(seconds)).append(" ثانیه");
        }
        return sb.toString();
    }

    /** Single Object overload: multiple overloads are ambiguous when called from SpEL. */
    public String digits(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(PERSIAN_DIGITS[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
