package io.github.aomckin.lifehud.domain;
import java.time.Instant;
public record AnimeWatchSession(String id, String animeId, int episodeStart, int episodeEnd, Instant watchedAt,
                                Integer durationMinutes, String note, Instant createdAt, Instant updatedAt) { }
