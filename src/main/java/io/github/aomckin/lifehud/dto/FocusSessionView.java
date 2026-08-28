package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.domain.FocusMode;
import io.github.aomckin.lifehud.domain.FocusSession;
import io.github.aomckin.lifehud.domain.FocusStatus;
import java.time.Instant;
import java.util.List;

public record FocusSessionView(String id, FocusMode mode, FocusStatus status, String title, String taskId,
        Instant startedAt, Instant endedAt, Integer plannedMinutes, long actualSeconds, long actualMinutes,
        long effectiveSeconds, long effectiveMinutes, String note, Instant updatedAt,
        List<String> relatedTaskIds, List<FocusSegmentView> segments, Integer breakMinutes) {
    public static FocusSessionView from(FocusSession session, Instant now) {
        long seconds = session.actualSeconds(now);
        List<FocusSegmentView> segments = session.segments() == null ? List.of() : session.segments().stream()
                .map(segment -> FocusSegmentView.from(segment, now)).toList();
        long effective = session.effectiveSeconds(now);
        return new FocusSessionView(session.id(), session.mode(), session.status(), session.title(),
                session.taskId(), session.startedAt(), session.endedAt(), session.plannedMinutes(),
                seconds, seconds / 60, effective, effective / 60, session.note(), session.updatedAt(),
                session.relatedTaskIds() == null ? List.of() : session.relatedTaskIds(), segments,
                session.breakMinutes() == null ? 5 : session.breakMinutes());
    }
}
