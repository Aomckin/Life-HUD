package io.github.aomckin.lifehud.domain;

public enum AchievementConditionType {
    ENERGY_REACH("energy_reach"),
    TASK_DONE_COUNT("task_done_count"),
    SPECIAL_TASK_DONE_COUNT("special_task_done_count"),
    ACTION_COUNT("action_count"),
    TOTAL_ACTION_COUNT("total_action_count"),
    LEVEL("level"),
    TASK_COMBO("task_combo");

    private final String jsonValue;

    AchievementConditionType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public static AchievementConditionType fromJson(String value) {
        for (AchievementConditionType type : values()) {
            if (type.jsonValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown achievement condition: " + value);
    }
}