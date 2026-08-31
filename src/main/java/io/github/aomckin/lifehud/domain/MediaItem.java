package io.github.aomckin.lifehud.domain;
import java.time.Instant;
public record MediaItem(String id, MediaItemType type, String title, MediaItemStatus status, Integer score,
                        Instant startedAt, Instant finishedAt, String coverImage, String note,
                        Instant createdAt, Instant updatedAt) { }
