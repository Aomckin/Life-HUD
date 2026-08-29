package io.github.aomckin.lifehud.domain;

/** Shared lifecycle for Dream and Goal; archiving is the preferred soft deletion. */
public enum DirectionStatus {
    ACTIVE, PAUSED, COMPLETED, ARCHIVED;

    /** Lenient parsing for request payloads; unknown or missing values fall back to ACTIVE. */
    public static DirectionStatus from(Object value) {
        if (value instanceof DirectionStatus status) return status;
        if (value != null) {
            try { return DirectionStatus.valueOf(String.valueOf(value).trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        return ACTIVE;
    }
}
