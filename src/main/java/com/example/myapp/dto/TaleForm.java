package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class TaleForm {

    @NotBlank(message = "لطفاً نام قصه را بنویسید")
    @Size(max = 150, message = "نام قصه خیلی طولانی است")
    private String title;

    @NotBlank(message = "لطفاً چند خط دربارهٔ قصه بنویسید")
    @Size(max = 4000, message = "توضیح خیلی طولانی است")
    private String description;

    @NotEmpty(message = "دست‌کم یک دسته برای قصه انتخاب کنید")
    private List<Long> categoryIds = new ArrayList<>();

    /** Required on create; optional on edit (keeps the existing audio). */
    private MultipartFile audio;

    /**
     * Filename returned by the progressive upload API. When set (and present in
     * the session's pending uploads), the form skips re-sending the audio blob.
     */
    private String audioFilename;

    private String audioContentType;

    private MultipartFile cover;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public MultipartFile getAudio() {
        return audio;
    }

    public void setAudio(MultipartFile audio) {
        this.audio = audio;
    }

    public String getAudioFilename() {
        return audioFilename;
    }

    public void setAudioFilename(String audioFilename) {
        this.audioFilename = audioFilename;
    }

    public String getAudioContentType() {
        return audioContentType;
    }

    public void setAudioContentType(String audioContentType) {
        this.audioContentType = audioContentType;
    }

    public MultipartFile getCover() {
        return cover;
    }

    public void setCover(MultipartFile cover) {
        this.cover = cover;
    }
}
