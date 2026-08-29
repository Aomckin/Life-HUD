package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/**
 * One of the ten 「现在。」歌单 slots. The audio file itself is stored once and shared;
 * snapshots freeze this whole record — including playCount, note and the wall layout —
 * so later slot changes never rewrite history.
 *
 * posX / posY are normalized (0~1) wall coordinates; rotationDeg stays within ±4°;
 * zIndex orders overlapping cards. A missing layout (nulls) means "not placed yet".
 */
public record NowSong(int slot, String filePath, String originalFilename, String title, String artist,
                      String album, int durationSeconds, String coverPath, String format,
                      int playCount, String note, Double posX, Double posY, Double rotationDeg, int zIndex,
                      Instant createdAt, Instant updatedAt) {
    public NowSong {
        note = note == null ? "" : note;
    }
}
