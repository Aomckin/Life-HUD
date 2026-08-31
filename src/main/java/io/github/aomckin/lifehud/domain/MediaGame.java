package io.github.aomckin.lifehud.domain;
import java.time.Instant;
public record MediaGame(String id, String title, String platform, MediaGameStatus status,
                        int basePlayTimeMinutes, int totalPlayTimeMinutes, Instant startedAt, Instant finishedAt,
                        Integer score, String coverImage, String note, Instant createdAt, Instant updatedAt) { }
