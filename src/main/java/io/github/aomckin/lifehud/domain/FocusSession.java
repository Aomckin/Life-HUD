package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** Durable source of truth for one period of intentional focus. */
public record FocusSession(String id, FocusMode mode, FocusStatus status, String title, String taskId,
        Instant startedAt, Instant activeSince, Instant endedAt, Integer plannedMinutes,
        long accumulatedSeconds, String note, Instant createdAt, Instant updatedAt,
        List<String> relatedTaskIds, List<FocusSegment> segments, Integer breakMinutes) {
    public long actualSeconds(Instant now) {
        if (status == FocusStatus.RUNNING && activeSince != null) {
            return accumulatedSeconds + Math.max(0, now.getEpochSecond() - activeSince.getEpochSecond());
        }
        return accumulatedSeconds;
    }

    public long effectiveSeconds(Instant now) {
        if (segments == null || segments.isEmpty()) return actualSeconds(now);
        return segments.stream().filter(segment -> segment.type() == FocusSegmentType.FOCUS)
                .mapToLong(segment -> segment.actualSeconds(now)).sum();
    }
}
