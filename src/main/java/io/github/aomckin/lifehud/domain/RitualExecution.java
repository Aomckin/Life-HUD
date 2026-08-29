package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** One real execution of a ritual; a completed execution must never emit its LifeEvent twice. */
public record RitualExecution(String id, String ritualId, String ritualName, Instant startedAt, Instant completedAt,
                              RitualExecutionStatus status, String note, List<Map<String, Object>> stepResults,
                              Instant createdAt, Instant updatedAt) {
    public RitualExecution {
        stepResults = stepResults == null ? List.of() : List.copyOf(stepResults);
    }
}
