package io.github.aomckin.lifehud.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Stable producer vocabulary for durable life events. */
public enum LifeEventSourceType {
    FOCUS, TASK, SYSTEM, ACHIEVEMENT, MILESTONE, LEVEL, RITUAL, SLEEP, MEAL, EXERCISE, CHECK_IN, LIFE_RECORD, MEDIA, DREAM, TITLE, JOURNAL, NOW, MANUAL;

    @JsonCreator
    public static LifeEventSourceType from(String value) {
        if (value == null || value.isBlank()) return SYSTEM;
        if ("growth".equalsIgnoreCase(value)) return LEVEL;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return SYSTEM; }
    }
}
