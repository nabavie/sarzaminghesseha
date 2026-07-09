package com.example.myapp.model;

public enum RequestStatus {
    PENDING("در انتظار بررسی", "warning"),
    APPROVED("پذیرفته شده", "success"),
    REJECTED("رد شده", "danger");

    private final String persianName;
    private final String badgeClass;

    RequestStatus(String persianName, String badgeClass) {
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
