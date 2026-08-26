package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.AchievementConditionType;
import io.github.aomckin.lifehud.domain.AchievementDefinition;
import io.github.aomckin.lifehud.domain.TaskRequirement;
import io.github.aomckin.lifehud.domain.TaskSource;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.AchievementRepository;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Evaluates typed achievement definitions against current game state. */
public final class AchievementSystem {
    private final Player player;
    private final LevelService levels;
    private final JsonFileStore files;
    private final List<AchievementDefinition> definitions;

    public AchievementSystem(Player player, LevelService levels, AchievementRepository repository,
                             JsonFileStore files) {
        this.player = player;
        this.levels = levels;
        this.files = files;
        this.definitions = repository.findAll();
    }

    /** Kept for direct callers created before the repository boundary existed. */
    public AchievementSystem(Player player, LevelService levels, JsonFileStore files,
                             com.fasterxml.jackson.databind.ObjectMapper ignoredMapper) {
        this(player, levels, new AchievementRepository(files), files);
    }

    public List<AchievementDefinition> checkDefinitions() {
        return definitions.stream()
                .filter(definition -> !player.unlocked_achievements.contains(definition.id()))
                .filter(this::isDone)
                .toList();
    }

    /** Legacy read adapter; application code uses {@link #checkDefinitions()}. */
    public List<Map<String, Object>> checkAchievements() {
        return checkDefinitions().stream().map(this::toLegacyMap).toList();
    }

    public List<AchievementDefinition> definitions() {
        return definitions;
    }

    /** Legacy view adapter for existing state assemblers. */
    public List<Map<String, Object>> items() {
        return definitions.stream().map(this::toLegacyMap).toList();
    }

    private boolean isDone(AchievementDefinition definition) {
        return switch (definition.conditionType()) {
            case ENERGY_REACH -> player.energy >= definition.targetValue();
            case TASK_DONE_COUNT -> player.done_task_count >= definition.targetValue();
            case SPECIAL_TASK_DONE_COUNT -> player.done_special_task_count >= definition.targetValue();
            case ACTION_COUNT -> player.action_counts.getOrDefault(definition.targetAction(), 0)
                    >= definition.targetValue();
            case TOTAL_ACTION_COUNT -> player.action_counts.values().stream().mapToInt(Integer::intValue).sum()
                    >= definition.targetValue();
            case LEVEL -> levels.level(player.exp) >= definition.targetValue();
            case TASK_COMBO -> hasCompletedCombo(definition.requirements());
        };
    }

    private boolean hasCompletedCombo(List<TaskRequirement> requirements) {
        if (requirements.isEmpty()) {
            return false;
        }
        Map<String, Integer> dailyCounts = completedCounts("tasks.json");
        Map<String, Integer> specialCounts = completedCounts("special_tasks.json");
        for (TaskRequirement requirement : requirements) {
            Map<String, Integer> counts = requirement.source() == TaskSource.DAILY ? dailyCounts : specialCounts;
            if (counts.getOrDefault(requirement.taskId(), 0) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> completedCounts(String fileName) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var task : files.read(fileName).path("tasks")) {
            if (task.hasNonNull("id")) {
                counts.put(task.path("id").asText(), task.path("completed_count").asInt());
            }
        }
        return counts;
    }

    private Map<String, Object> toLegacyMap(AchievementDefinition definition) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", definition.id());
        result.put("name", definition.name());
        result.put("desc", definition.description());
        result.put("condition_type", definition.conditionType().name().toLowerCase());
        result.put("target_action", definition.targetAction());
        result.put("target_value", definition.targetValue());
        result.put("reward_coin", definition.rewardCoin());
        result.put("reward_exp", definition.rewardExp());
        return result;
    }
}