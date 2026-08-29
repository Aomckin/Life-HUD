package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * The single image storage for every module (Dream covers, Now images, future ones).
 * Files live under data/uploads/ with random UUID names; only image types are accepted.
 */
@Service
public final class ImageStorageService {
    private static final Set<String> ALLOWED = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final long MAX_BYTES = 8 * 1024 * 1024;
    public static final String PUBLIC_PREFIX = "/uploads/";
    private final JsonFileStore files;

    public ImageStorageService(JsonFileStore files) { this.files = files; }

    /** Stores an upload and returns its public URL path (e.g. /uploads/<uuid>.png). */
    public String store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw bad("没有收到文件");
        if (upload.getSize() > MAX_BYTES) throw bad("图片不能超过 8MB");
        String contentType = upload.getContentType() == null ? "" : upload.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(contentType)) throw bad("只支持 PNG / JPG / WebP / GIF 图片");
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".gif";
        };
        try {
            Path directory = files.resolve("uploads");
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + extension).normalize();
            if (!target.startsWith(directory)) throw bad("非法的存储路径");
            try (var input = upload.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return PUBLIC_PREFIX + target.getFileName();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", e);
        }
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
