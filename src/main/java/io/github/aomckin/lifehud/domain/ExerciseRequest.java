package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

public record ExerciseRequest(String type, Instant startTime, Integer durationMinutes, String intensity, String note,
                              List<String> images) {
    public ExerciseRequest(String type, Instant startTime, Integer durationMinutes, String intensity, String note) {
        this(type, startTime, durationMinutes, intensity, note, null);
    }
}
