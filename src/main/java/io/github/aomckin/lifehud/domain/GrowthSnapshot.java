package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalDate;

public record GrowthSnapshot(LocalDate date, int level, int totalExp, int energy,
                             long totalFocusMinutes, long totalEffectiveFocusMinutes,
                             int totalTaskCompleted, int lifeEventCount,
                             int achievementCount, int milestoneCount, Instant createdAt) { }
