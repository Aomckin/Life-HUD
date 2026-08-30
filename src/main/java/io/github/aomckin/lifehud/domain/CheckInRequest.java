package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

public record CheckInRequest(Integer energy, Integer mood, Integer focusDesire, Integer fatigue,
                             Instant time, String note, List<String> images) {
    public CheckInRequest(Integer energy, Integer mood, Integer focusDesire, Integer fatigue,
                          Instant time, String note) {
        this(energy, mood, focusDesire, fatigue, time, note, null);
    }
}
