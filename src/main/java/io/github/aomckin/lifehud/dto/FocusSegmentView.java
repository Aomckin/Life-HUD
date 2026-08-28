package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.domain.FocusSegment;
import io.github.aomckin.lifehud.domain.FocusSegmentType;
import java.time.Instant;

public record FocusSegmentView(String id, FocusSegmentType type, String title, Instant startedAt,
        Instant endedAt, long actualSeconds, String relatedTaskId, String note, int order, boolean active) {
    public static FocusSegmentView from(FocusSegment segment, Instant now) {
        return new FocusSegmentView(segment.id(), segment.type(), segment.title(), segment.startedAt(),
                segment.endedAt(), segment.actualSeconds(now), segment.relatedTaskId(), segment.note(),
                segment.order(), segment.active());
    }
}
