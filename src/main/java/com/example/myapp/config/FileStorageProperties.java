package com.example.myapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.upload")
public class FileStorageProperties {

    /** Root directory for uploaded files, relative to the working directory. */
    private String dir = "uploads";

    /**
     * Largest audio file a storyteller may send. Keep it below
     * {@code spring.servlet.multipart.max-file-size} so the friendly Persian message
     * wins over the container's generic "request too large" failure.
     */
    private DataSize maxAudioSize = DataSize.ofMegabytes(10);

    /** Largest cover or avatar image. */
    private DataSize maxImageSize = DataSize.ofMegabytes(3);

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public DataSize getMaxAudioSize() {
        return maxAudioSize;
    }

    public void setMaxAudioSize(DataSize maxAudioSize) {
        this.maxAudioSize = maxAudioSize;
    }

    public DataSize getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(DataSize maxImageSize) {
        this.maxImageSize = maxImageSize;
    }
}
