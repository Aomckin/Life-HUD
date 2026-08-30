package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/**
 * A 10-second state snapshot before/after doing things. All four scales are
 * 1~10; this is how the future agent context will read the user's state, not
 * a live telemetry feed.
 */
public record CheckIn(String id, int energy, int mood, int focusDesire, int fatigue,
                      Instant time, String note, List<String> images, Instant createdAt, Instant updatedAt) {
    public CheckIn { images = images == null ? List.of() : List.copyOf(images); }
}
