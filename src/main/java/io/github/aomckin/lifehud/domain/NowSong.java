package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/**
 * One of the ten "「现在。」歌单" slots. The audio file itself is stored once and shared;
 * snapshots freeze this whole record so later slot changes never rewrite history.
 */
public record NowSong(int slot, String filePath, String originalFilename, String title, String artist,
                      String album, int durationSeconds, String coverPath, String format, Instant uploadedAt) { }
