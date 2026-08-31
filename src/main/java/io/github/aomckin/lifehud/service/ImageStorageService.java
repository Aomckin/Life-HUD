package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
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
    private static final long MAX_BYTES = 64L * 1024 * 1024; // matches the multipart limit in application.yml
    public static final String PUBLIC_PREFIX = "/uploads/";
    private final UploadStorageService uploads;

    public ImageStorageService(JsonFileStore files,UploadStorageService uploads) { this.uploads = uploads; }

    /** Stores an upload and returns its public URL path (e.g. /uploads/<uuid>.png). */
    public String store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw bad("没有收到文件");
        if (upload.getSize() > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "图片不能超过 64MB");
        String contentType = upload.getContentType() == null ? "" : upload.getContentType().toLowerCase(Locale.ROOT);
        String name = upload.getOriginalFilename() == null ? "" : upload.getOriginalFilename().toLowerCase(Locale.ROOT);
        // Browsers send real image MIME types, but some clients only send octet-stream — fall back to the extension.
        if (!ALLOWED.contains(contentType) && !BY_EXTENSION.contains(extensionOf(name)))
            throw bad("只支持 PNG / JPG / WebP / GIF 图片");
        String extension = ALLOWED.contains(contentType) ? extensionFromMime(contentType) : extensionOf(name);
        try {
            return uploads.store(upload.getBytes(),extension);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", e);
        }
    }

    private static final Set<String> BY_EXTENSION = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }
    private String extensionFromMime(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".gif";
        };
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
