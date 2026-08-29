package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.NowSong;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Real audio upload for the 「现在。」歌单: stores the file under data/uploads/ and reads
 * as much media metadata as the tags provide (title / artist / album / duration / embedded
 * cover) with jaudiotagger. Missing tags fall back to filename / 未知艺术家 — the frontend
 * never guesses song info from filenames. Audio files are never overwritten in place so
 * old snapshots keep rendering.
 */
@Service
public final class AudioStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "flac");
    private final JsonFileStore files;

    public AudioStorageService(JsonFileStore files) { this.files = files; }

    public NowSong store(MultipartFile upload, int slot) {
        if (upload == null || upload.isEmpty()) throw bad("没有收到音频文件");
        String original = upload.getOriginalFilename() == null ? "" : upload.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw bad("只支持 MP3 / FLAC 音频文件");
        try {
            Path directory = files.resolve("uploads");
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) throw bad("非法的存储路径");
            try (var input = upload.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            Metadata metadata = readMetadata(target, baseName(original));
            return new NowSong(slot, "/uploads/" + storedName, original, metadata.title(), metadata.artist(),
                    metadata.album(), metadata.durationSeconds(), metadata.coverPath(), extension.toUpperCase(Locale.ROOT),
                    Instant.now());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "音频保存失败", e);
        }
    }

    private Metadata readMetadata(Path file, String fallbackTitle) {
        String title = fallbackTitle;
        String artist = "未知艺术家";
        String album = "";
        int durationSeconds = 0;
        String coverPath = "";
        try {
            AudioFile audio = AudioFileIO.read(file.toFile());
            durationSeconds = audio.getAudioHeader().getTrackLength();
            Tag tag = audio.getTag();
            if (tag != null) {
                String taggedTitle = tag.getFirst(FieldKey.TITLE);
                if (notBlank(taggedTitle)) title = taggedTitle.trim();
                String taggedArtist = tag.getFirst(FieldKey.ARTIST);
                if (notBlank(taggedArtist)) artist = taggedArtist.trim();
                String taggedAlbum = tag.getFirst(FieldKey.ALBUM);
                if (notBlank(taggedAlbum)) album = taggedAlbum.trim();
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null && artwork.getBinaryData() != null) {
                    String imageExtension = imageExtensionOf(artwork.getMimeType());
                    String coverName = UUID.randomUUID() + imageExtension;
                    Path coverTarget = files.resolve("uploads").resolve(coverName).normalize();
                    Files.write(coverTarget, artwork.getBinaryData());
                    coverPath = "/uploads/" + coverName;
                }
            }
        } catch (Exception ignored) {
            // Unreadable or untagged file: keep the filename-based fallbacks above.
        }
        return new Metadata(title, artist, album, durationSeconds, coverPath);
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    private String baseName(String name) {
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        return slash < 0 ? base : base.substring(slash + 1);
    }
    private String imageExtensionOf(String mimeType) {
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (normalized.contains("png")) return ".png";
        if (normalized.contains("gif")) return ".gif";
        return ".jpg";
    }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private record Metadata(String title, String artist, String album, int durationSeconds, String coverPath) { }
}
