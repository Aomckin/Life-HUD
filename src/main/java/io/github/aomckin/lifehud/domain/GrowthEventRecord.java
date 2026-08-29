package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** Durable receipt proving that one LifeEvent was applied to growth exactly once. */
public record GrowthEventRecord(String eventId, Instant processedAt, int expDelta, int energyDelta,
                                int levelBefore, int levelAfter, String result,
                                List<String> unlockedAchievements, List<String> unlockedTitles) {
    public GrowthEventRecord {
        unlockedAchievements = List.copyOf(unlockedAchievements);
        unlockedTitles = List.copyOf(unlockedTitles);
    }
}
