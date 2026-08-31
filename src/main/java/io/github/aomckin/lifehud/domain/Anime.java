package io.github.aomckin.lifehud.domain;
import java.time.Instant;
public record Anime(String id, String title, int totalEpisodes, int currentEpisode, AnimeStatus status,
                    Integer score, Instant startedAt, Instant finishedAt, String coverImage, String note,
                    Instant createdAt, Instant updatedAt) { }
