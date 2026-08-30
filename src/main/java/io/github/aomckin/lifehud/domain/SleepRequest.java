package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** Create/update payload for SleepRecord; null fields keep their value on update. */
public record SleepRequest(Instant sleepTime, Instant wakeTime, Integer quality, String type, String note,
                           List<String> images) {
    public SleepRequest(Instant sleepTime, Instant wakeTime, Integer quality, String type, String note) {
        this(sleepTime, wakeTime, quality, type, note, null);
    }
}
