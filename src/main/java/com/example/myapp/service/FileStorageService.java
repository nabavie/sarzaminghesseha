package com.example.myapp.service;

import com.example.myapp.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final String AUDIO = "audio";
    public static final String COVERS = "covers";
    public static final String AVATARS = "avatars";

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "m4a", "ogg", "webm", "wav");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path root;

    public FileStorageService(FileStorageProperties properties) {
        this.root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root.resolve(AUDIO));
            Files.createDirectories(root.resolve(COVERS));
            Files.createDirectories(root.resolve(AVATARS));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directories at " + root, e);
        }
    }

    public String storeAudio(MultipartFile file) {
        return store(file, AUDIO, AUDIO_EXTENSIONS,
                "فرمت فایل صوتی پشتیبانی نمی‌شود؛ لطفاً فایل mp3، m4a، ogg، webm یا wav انتخاب کنید");
    }

    public String storeCover(MultipartFile file) {
        return store(file, COVERS, IMAGE_EXTENSIONS,
                "فرمت تصویر پشتیبانی نمی‌شود؛ لطفاً عکس jpg، png یا webp انتخاب کنید");
    }

    public String storeAvatar(MultipartFile file) {
        return store(file, AVATARS, IMAGE_EXTENSIONS,
                "فرمت تصویر پشتیبانی نمی‌شود؛ لطفاً عکس jpg، png یا webp انتخاب کنید");
    }

    private String store(MultipartFile file, String subdir, Set<String> allowedExtensions, String errorMessage) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(errorMessage);
        }
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), root.resolve(subdir).resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file in " + subdir, e);
        }
        return filename;
    }

    /** Stores a raw stream (used by seeders); returns the generated filename. */
    public String storeStream(java.io.InputStream in, String subdir, String extension) {
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(in, root.resolve(subdir).resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store seed file in " + subdir, e);
        }
        return filename;
    }

    public Resource load(String subdir, String filename) {
        Path path = root.resolve(subdir).resolve(filename).normalize();
        // filenames are server-generated UUIDs, but guard against traversal anyway
        if (!path.startsWith(root)) {
            return null;
        }
        FileSystemResource resource = new FileSystemResource(path);
        return resource.exists() ? resource : null;
    }

    public void delete(String subdir, String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        Path path = root.resolve(subdir).resolve(filename).normalize();
        if (!path.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file " + path, e);
        }
    }
}
