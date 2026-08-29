package io.github.aomckin.lifehud.domain;

/** Step types of a ritual execution flow. */
public enum RitualStepType {
    TEXT, CHECK, TIMER, LINK, MUSIC_HINT, NOTE;

    /** Lenient parsing for request payloads; unknown or missing values fall back to TEXT. */
    public static RitualStepType from(Object value) {
        if (value instanceof RitualStepType type) return type;
        if (value != null) {
            try { return RitualStepType.valueOf(String.valueOf(value).trim().toUpperCase().replace(' ', '_')); }
            catch (IllegalArgumentException ignored) { }
        }
        return TEXT;
    }
}
