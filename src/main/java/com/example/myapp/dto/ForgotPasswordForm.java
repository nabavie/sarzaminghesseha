package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ForgotPasswordForm {

    @NotBlank(message = "لطفاً نام کاربری خود را بنویسید")
    private String username;

    @NotBlank(message = "لطفاً کد بازیابی را بنویسید")
    private String recoveryCode;

    @NotBlank(message = "لطفاً رمز عبور تازه را بنویسید")
    @Size(min = 8, max = 100, message = "رمز عبور باید دست‌کم ۸ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "رمز عبور باید دست‌کم یک حرف و یک عدد داشته باشد")
    private String newPassword;

    @NotBlank(message = "لطفاً رمز عبور را دوباره بنویسید")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRecoveryCode() {
        return recoveryCode;
    }

    public void setRecoveryCode(String recoveryCode) {
        this.recoveryCode = recoveryCode;
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
