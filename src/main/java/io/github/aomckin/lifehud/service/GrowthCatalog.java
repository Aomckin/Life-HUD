package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import java.util.List;
import java.util.Map;

/** Small v0.4 catalog grounded in modules that exist today. */
public final class GrowthCatalog {
    private GrowthCatalog() { }
    public static final List<AchievementRule> ACHIEVEMENTS = List.of(
            rule("iron_first", "铁幕初启", "第一道幕布已经落下。", "FOCUS", false, GrowthConditionType.COUNT, GrowthMetric.IRON_FOCUS_COUNT, 1, "iron_walker"),
            rule("iron_ten", "幕布之后", "十次铁幕，已经形成自己的节奏。", "FOCUS", false, GrowthConditionType.COUNT, GrowthMetric.IRON_FOCUS_COUNT, 10, null),
            rule("focus_hundred_hours", "百时", "一百小时，已经真正留在这里。", "FOCUS", false, GrowthConditionType.TOTAL_DURATION, GrowthMetric.EFFECTIVE_FOCUS_MINUTES, 6000, "hundred_hours"),
            rule("focus_hundred", "专注者", "一百次进入专注，也是一条清晰的轨迹。", "FOCUS", false, GrowthConditionType.COUNT, GrowthMetric.FOCUS_COUNT, 100, null),
            rule("long_night", "长夜", "一次专注跨过了午夜。", "FOCUS", true, GrowthConditionType.COUNT, GrowthMetric.CROSS_MIDNIGHT_FOCUS_COUNT, 1, "night_voyager"),
            rule("deep_dive", "深潜", "单次有效 Focus 达到三小时。", "FOCUS", false, GrowthConditionType.SINGLE_DURATION, GrowthMetric.MAX_EFFECTIVE_FOCUS_MINUTES, 180, null),
            rule("pomodoro_first", "番茄熟了", "完成第一次 Pomodoro。", "FOCUS", false, GrowthConditionType.COUNT, GrowthMetric.POMODORO_FOCUS_COUNT, 1, null),
            rule("task_first", "小小一步", "第一个 Task 被认真完成。", "TASK", false, GrowthConditionType.COUNT, GrowthMetric.TASK_COUNT, 1, null),
            rule("task_hundred", "百件", "一百件小事共同留下了方向。", "TASK", false, GrowthConditionType.COUNT, GrowthMetric.TASK_COUNT, 100, null),
            rule("events_hundred", "轨迹开始", "第一百条 LifeEvent 留在时间线里。", "LIFE", false, GrowthConditionType.COUNT, GrowthMetric.LIFE_EVENT_COUNT, 100, "life_recorder"),
            rule("events_thousand", "生活档案", "一千个时刻构成了一段生活。", "LIFE", false, GrowthConditionType.COUNT, GrowthMetric.LIFE_EVENT_COUNT, 1000, null),
            rule("milestone_first", "拾起一刻", "第一个由你选择的里程碑。", "LIFE", false, GrowthConditionType.COUNT, GrowthMetric.MILESTONE_COUNT, 1, "milestone_keeper"),
            rule("level_five", "渐有轮廓", "长期积累开始显露形状。", "GROWTH", false, GrowthConditionType.LEVEL, GrowthMetric.LEVEL, 5, null),
            rule("level_ten", "行至十阶", "这不是终点，只是一枚刻度。", "GROWTH", false, GrowthConditionType.LEVEL, GrowthMetric.LEVEL, 10, null)
    );
    public static final List<TitleDefinition> TITLES = List.of(
            new TitleDefinition("iron_walker", "铁幕行者", "在一次次开幕与落幕之间前行。", "ACHIEVEMENT", "iron_first", false, false),
            new TitleDefinition("night_voyager", "夜航者", "曾在长夜里保持自己的航向。", "ACHIEVEMENT", "long_night", true, false),
            new TitleDefinition("hundred_hours", "百时专注", "一百小时的有效专注已经留下。", "ACHIEVEMENT", "focus_hundred_hours", false, false),
            new TitleDefinition("life_recorder", "生活记录者", "认真保存生活留下的痕迹。", "ACHIEVEMENT", "events_hundred", false, false),
            new TitleDefinition("milestone_keeper", "里程碑收藏家", "知道哪些时刻值得被留下。", "ACHIEVEMENT", "milestone_first", false, false)
    );
    public record AchievementRule(String id,String name,String description,String category,boolean hidden,
                                  String icon, GrowthAchievementCondition condition,String relatedTitleId) { }
    public record TitleDefinition(String id,String name,String description,String sourceType,String sourceId,
                                  boolean hidden,boolean custom) { }
    private static AchievementRule rule(String id, String name, String description, String category, boolean hidden,
                                        GrowthConditionType type, GrowthMetric metric, long target, String title) {
        return new AchievementRule(id, name, description, category, hidden, "◇",
                new GrowthAchievementCondition(type, metric, target, Map.of()), title);
    }
}
