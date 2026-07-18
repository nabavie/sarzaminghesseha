package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "لطفاً نام خود را بنویسید")
    @Size(max = 100, message = "نام خیلی طولانی است")
    private String displayName;

    @NotBlank(message = "لطفاً یک نام کاربری انتخاب کنید")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$",
            message = "نام کاربری باید با حروف انگلیسی، عدد یا خط زیر باشد (۳ تا ۳۰ حرف)")
    private String username;

    @NotBlank(message = "لطفاً یک رمز عبور انتخاب کنید")
    @Size(min = 8, max = 100, message = "رمز عبور باید دست‌کم ۸ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "رمز عبور باید دست‌کم یک حرف و یک عدد داشته باشد")
    private String password;

    @NotBlank(message = "لطفاً رمز عبور را دوباره بنویسید")
    private String confirmPassword;

    /** Optional; validated/normalized in AuthController via MobileNumbers. */
    @Size(max = 20, message = "شماره موبایل خیلی طولانی است")
    private String mobile;

    private boolean storyteller;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isStoryteller() {
        return storyteller;
    }

    public void setStoryteller(boolean storyteller) {
        this.storyteller = storyteller;
    }
}
