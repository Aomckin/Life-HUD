package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** Incremental counters used by Growth reads and achievement evaluation. */
public record GrowthStats(long ironFocusCount, long pomodoroFocusCount, long focusCount,
                          long actualFocusMinutes, long effectiveFocusMinutes, long maxEffectiveFocusMinutes,
                          long crossMidnightFocusCount, long taskCount, long lifeEventCount,
                          long milestoneCount, Instant updatedAt, int schemaVersion) {
    public static GrowthStats empty() {
        return new GrowthStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Instant.EPOCH, 1);
    }
}
