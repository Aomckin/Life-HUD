package io.github.aomckin.lifehud.domain;

import java.time.Instant;

public record ExerciseRequest(String type, Instant startTime, Integer durationMinutes, String intensity, String note) { }
