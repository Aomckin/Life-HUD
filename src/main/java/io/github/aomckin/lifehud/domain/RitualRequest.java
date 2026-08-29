package io.github.aomckin.lifehud.domain;

import java.time.LocalTime;
import java.util.List;

/** Request body for creating or updating a ritual together with its ordered steps. */
public record RitualRequest(String name, String description, String category, LocalTime triggerTime,
                            boolean enabled, List<RitualStepInput> steps) {
    public record RitualStepInput(RitualStepType type, String title, String content,
                                  Integer durationSeconds, String url, Integer sortOrder, Boolean required) { }
}
