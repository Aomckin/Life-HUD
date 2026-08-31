package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.repository.JsonFileStore;
import io.github.aomckin.lifehud.domain.NowSong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioStorageServiceTest {
    @TempDir Path temp;
    JsonFileStore files; AudioStorageService audio;

    @BeforeEach void setup() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        files = new JsonFileStore(mapper, temp);
        audio = new AudioStorageService(files,new UploadStorageService(files,mapper));
    }

    /** Missing tags fall back to filename / 未知艺术家 instead of guessing from the filename. */
    @Test void untaggedFileFallsBackToFilenameAndUnknownArtist() {
        MockMultipartFile file = new MockMultipartFile("file", "我的录音.mp3", "audio/mpeg", new byte[] {1, 2, 3, 4});
        NowSong song = audio.store(file, 3);
        assertThat(song.title()).isEqualTo("我的录音");
        assertThat(song.artist()).isEqualTo("未知艺术家");
        assertThat(song.album()).isEmpty();
        assertThat(song.slot()).isEqualTo(3);
        assertThat(song.format()).isEqualTo("MP3");
        assertThat(song.filePath()).startsWith("/uploads/");
        assertThat(files.exists(song.filePath().replace("/uploads/", "uploads/"))).isTrue();
    }

    /** An ID3v2.3-tagged MP3 yields its real title / artist / album and the embedded cover. */
    @Test void taggedMp3ReadsTitleArtistAlbumAndCover() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        byte[] mp3 = taggedMp3("Sunny", "Jay Chou", "Yehui Mei", png);
        MockMultipartFile file = new MockMultipartFile("file", "sunny.mp3", "audio/mpeg", mp3);
        NowSong song = audio.store(file, 1);
        assertThat(song.title()).isEqualTo("Sunny");
        assertThat(song.artist()).isEqualTo("Jay Chou");
        assertThat(song.album()).isEqualTo("Yehui Mei");
        assertThat(song.durationSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(song.coverPath()).startsWith("/uploads/");
        assertThat(files.exists(song.coverPath().replace("/uploads/", "uploads/"))).isTrue();
    }

    @Test void rejectsNonAudioExtensions() {
        MockMultipartFile file = new MockMultipartFile("file", "song.txt", "text/plain", new byte[] {1});
        assertThatThrownBy(() -> audio.store(file, 1)).hasMessageContaining("MP3 / FLAC");
    }

    @Test void reusesAnExistingFileWhenOnlyTheUploadNameDiffers() {
        byte[] content={1,2,3,4};
        NowSong first=audio.store(new MockMultipartFile("file","first.mp3","audio/mpeg",content),1);
        NowSong second=audio.store(new MockMultipartFile("file","renamed.mp3","audio/mpeg",content),2);
        assertThat(second.filePath()).isEqualTo(first.filePath());
        assertThat(temp.resolve("uploads").toFile().listFiles()).hasSize(1);
    }

    /** Builds a minimal MP3: an ID3v2.3 tag (TIT2/TPE1/TALB/APIC) followed by one MPEG frame. */
    private byte[] taggedMp3(String title, String artist, String album, byte[] coverPng) {
        ByteArrayOutputStream frames = new ByteArrayOutputStream();
        frames.writeBytes(textFrame("TIT2", title));
        frames.writeBytes(textFrame("TPE1", artist));
        frames.writeBytes(textFrame("TALB", album));
        frames.writeBytes(apicFrame(coverPng));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {'I', 'D', '3', 3, 0, 0});
        out.writeBytes(syncsafe(frames.size()));
        out.writeBytes(frames.toByteArray());
        out.writeBytes(mpegFrames());
        return out.toByteArray();
    }

    private byte[] textFrame(String id, String value) {
        byte[] text = value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.writeBytes(id.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        frame.writeBytes(be32(text.length + 1));
        frame.writeBytes(new byte[] {0, 0});
        frame.write(0);
        frame.writeBytes(text);
        return frame.toByteArray();
    }

    private byte[] apicFrame(byte[] png) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(0);
        body.writeBytes("image/png".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        body.write(0);
        body.write(0);
        body.write(0);
        body.writeBytes(png);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.writeBytes("APIC".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        frame.writeBytes(be32(body.size()));
        frame.writeBytes(new byte[] {0, 0});
        frame.writeBytes(body.toByteArray());
        return frame.toByteArray();
    }

    private byte[] be32(int value) {
        return new byte[] {(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    private byte[] syncsafe(int value) {
        return new byte[] {(byte) ((value >> 21) & 0x7F), (byte) ((value >> 14) & 0x7F),
                (byte) ((value >> 7) & 0x7F), (byte) (value & 0x7F)};
    }

    /** jaudiotagger's header scan needs several consecutive valid MPEG frames. */
    private byte[] mpegFrames() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < 40; i++) {
            byte[] frame = new byte[417];
            frame[0] = (byte) 0xFF; frame[1] = (byte) 0xFB; frame[2] = (byte) 0x90; frame[3] = 0x00;
            Arrays.fill(frame, 4, frame.length, (byte) 0);
            out.writeBytes(frame);
        }
        return out.toByteArray();
    }
}
