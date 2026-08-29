package io.github.aomckin.lifehud.domain;

/** One ordered step inside a ritual; v0.5 keeps it free of music-service integration. */
public record RitualStep(String id, String ritualId, RitualStepType type, String title, String content,
                         int durationSeconds, String url, int sortOrder, boolean required) { }
