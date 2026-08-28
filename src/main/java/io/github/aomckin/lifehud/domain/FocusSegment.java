package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** One non-overlapping piece of work, rest, or interruption inside a Focus session. */
public record FocusSegment(String id, FocusSegmentType type, String title, Instant startedAt,
        Instant endedAt, String relatedTaskId, String note, int order, Instant createdAt, Instant updatedAt) {
    public long actualSeconds(Instant now) {
        Instant end = endedAt == null ? now : endedAt;
        return Math.max(0, end.getEpochSecond() - startedAt.getEpochSecond());
    }

    public boolean active() { return endedAt == null; }
}
