package io.github.aomckin.lifehud.domain;

import java.time.LocalDate;

public record GrowthTrendPoint(LocalDate date, long effectiveFocusMinutes, int expDelta,
                               int taskDelta, int energy, int achievementCount, int milestoneCount) { }
