package io.github.aomckin.lifehud.domain;

import java.util.Map;

public record GrowthAchievementCondition(GrowthConditionType type, GrowthMetric metric,
                                         long target, Map<String, Object> metadata) {
    public GrowthAchievementCondition {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (target < 0) throw new IllegalArgumentException("target must be non-negative");
    }
}
