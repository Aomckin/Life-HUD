package io.github.aomckin.lifehud.domain;

/** Lifecycle of a DreamMilestone: a staged node under a Goal, distinct from the Growth Milestone. */
public enum DreamMilestoneStatus {
    PENDING, COMPLETED, ARCHIVED;

    /** Lenient parsing for request payloads; unknown or missing values fall back to PENDING. */
    public static DreamMilestoneStatus from(Object value) {
        if (value instanceof DreamMilestoneStatus status) return status;
        if (value != null) {
            try { return DreamMilestoneStatus.valueOf(String.valueOf(value).trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        return PENDING;
    }
}
