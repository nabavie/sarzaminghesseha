package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SiteFeedbackForm {

    @NotBlank(message = "لطفاً نام خود را بنویسید")
    @Size(max = 100, message = "نام خیلی طولانی است")
    private String name;

    @NotBlank(message = "لطفاً نظر یا پیشنهاد خود را بنویسید")
    @Size(max = 2000, message = "متن خیلی طولانی است")
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
