package com.example.myapp.model;

public enum TaleStatus {
    PENDING("در انتظار بررسی", "warning"),
    APPROVED("منتشر شده", "success"),
    REJECTED("رد شده", "danger");

    private final String persianName;
    private final String badgeClass;

    TaleStatus(String persianName, String badgeClass) {
        this.persianName = persianName;
        this.badgeClass = badgeClass;
    }

    public String getPersianName() {
        return persianName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
