package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/**
 * One sleep fact. durationMinutes is always computed from sleepTime → wakeTime
 * server-side, so cross-midnight sleeps and backfilled records can never drift
 * from the timestamps.
 */
public record SleepRecord(String id, Instant sleepTime, Instant wakeTime, int durationMinutes,
                          int quality, SleepType type, String note, List<String> images,
                          Instant createdAt, Instant updatedAt) {
    public SleepRecord { images = images == null ? List.of() : List.copyOf(images); }
}
