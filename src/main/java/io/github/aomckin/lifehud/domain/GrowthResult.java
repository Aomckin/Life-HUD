package io.github.aomckin.lifehud.domain;

import java.util.List;

public record GrowthResult(String eventId, int expDelta, int energyDelta, int levelBefore,
                           int levelAfter, boolean levelUp, List<String> unlockedAchievements,
                           List<String> unlockedTitles, String message, boolean duplicate) {
    public GrowthResult {
        unlockedAchievements = List.copyOf(unlockedAchievements);
        unlockedTitles = List.copyOf(unlockedTitles);
    }
}
