package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/**
 * One sleep fact. durationMinutes is always computed from sleepTime → wakeTime
 * server-side, so cross-midnight sleeps and backfilled records can never drift
 * from the timestamps.
 */
public record SleepRecord(String id, Instant sleepTime, Instant wakeTime, int durationMinutes,
                          int quality, SleepType type, String note, Instant createdAt, Instant updatedAt) { }
