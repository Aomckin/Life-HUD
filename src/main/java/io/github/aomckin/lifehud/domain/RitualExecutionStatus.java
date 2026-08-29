package io.github.aomckin.lifehud.domain;

/** Lifecycle of one ritual execution. */
public enum RitualExecutionStatus {
    RUNNING, COMPLETED, CANCELLED;

    /** Lenient parsing for request payloads; unknown or missing values fall back to RUNNING. */
    public static RitualExecutionStatus from(Object value) {
        if (value instanceof RitualExecutionStatus status) return status;
        if (value != null) {
            try { return RitualExecutionStatus.valueOf(String.valueOf(value).trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        return RUNNING;
    }
}
