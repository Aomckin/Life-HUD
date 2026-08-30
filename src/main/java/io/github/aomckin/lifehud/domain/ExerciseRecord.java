package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** One exercise fact: what, when it started, how long, how hard. */
public record ExerciseRecord(String id, ExerciseType type, Instant startTime, int durationMinutes,
                             ExerciseIntensity intensity, String note, List<String> images,
                             Instant createdAt, Instant updatedAt) {
    public ExerciseRecord { images = images == null ? List.of() : List.copyOf(images); }
}
