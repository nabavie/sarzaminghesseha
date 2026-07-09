package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {

    @NotBlank(message = "لطفاً رمز عبور فعلی را بنویسید")
    private String currentPassword;

    @NotBlank(message = "لطفاً رمز عبور تازه را بنویسید")
    @Size(min = 8, max = 100, message = "رمز عبور باید دست‌کم ۸ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "رمز عبور باید دست‌کم یک حرف و یک عدد داشته باشد")
    private String newPassword;

    @NotBlank(message = "لطفاً رمز عبور تازه را دوباره بنویسید")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
