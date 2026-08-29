package io.github.aomckin.lifehud.domain;

import java.time.Instant;

public record CheckInRequest(Integer energy, Integer mood, Integer focusDesire, Integer fatigue,
                             Instant time, String note) { }
