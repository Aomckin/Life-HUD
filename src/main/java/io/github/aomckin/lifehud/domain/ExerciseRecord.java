package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** One exercise fact: what, when it started, how long, how hard. */
public record ExerciseRecord(String id, ExerciseType type, Instant startTime, int durationMinutes,
                             ExerciseIntensity intensity, String note, Instant createdAt, Instant updatedAt) { }
