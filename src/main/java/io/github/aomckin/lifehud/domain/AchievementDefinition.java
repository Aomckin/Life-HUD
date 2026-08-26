package io.github.aomckin.lifehud.domain;

import java.util.List;

public record AchievementDefinition(
        String id,
        String name,
        String description,
        AchievementConditionType conditionType,
        int targetValue,
        String targetAction,
        List<TaskRequirement> requirements,
        int rewardCoin,
        int rewardExp) {
    public AchievementDefinition {
        requirements = List.copyOf(requirements);
    }
}