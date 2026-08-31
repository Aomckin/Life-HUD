package io.github.aomckin.lifehud.domain;
import java.time.Instant;
public record MediaGameSession(String id, String gameId, Instant startTime, Instant endTime, int durationMinutes,
                               String progress, String note, Instant createdAt, Instant updatedAt) { }
