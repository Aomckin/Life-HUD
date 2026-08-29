package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.core.ActionCatalog;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.AchievementDefinition;
import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.domain.SpecialTask;
import io.github.aomckin.lifehud.repository.LogRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Converts domain/application read models into the existing front-end payload. */
public final class GameViewAssembler {
    private final GameConfig config;
    private final ActionCatalog actions;
    private final Player player;
    private final ObjectMapper mapper;
    private final LevelService levels;
    private final LogRepository logs;
    private final DailyTaskManager daily;
    private final SpecialTaskManager special;
    private final ShopManager shop;
    private final AchievementSystem achievements;
    private final TitleSystem titles;

    public GameViewAssembler(GameConfig config, ActionCatalog actions, Player player, ObjectMapper mapper,
                             LevelService levels, LogRepository logs, DailyTaskManager daily,
                             SpecialTaskManager special, ShopManager shop,
                             AchievementSystem achievements, TitleSystem titles) {
        this.config = config; this.actions = actions; this.player = player; this.mapper = mapper;
        this.levels = levels; this.logs = logs; this.daily = daily; this.special = special;
        this.shop = shop; this.achievements = achievements; this.titles = titles;
    }

    public GameStateView assemble() {
        Map<String, Object> state = map();
        state.put("window_title", config.windowTitle());
        state.put("window_size", config.windowSize());
        state.put("energy_text", "宅宅能量：" + player.energy + "/" + player.maxEnergy);
        state.put("energy_value", player.energy); state.put("energy_max", player.maxEnergy);
        state.put("exp_text", levels.expText(player.exp)); state.put("level_text", levels.levelText(player.exp));
        state.put("title_text", "称号：" + titles.name());
        state.put("logs", logs.recent()); state.put("action_views", actionViews());
        state.put("active_task_views", dailyViews(daily.tasks()));
        state.put("active_special_task_views", specialViews(special.tasks()));
        state.put("achievement_sections", achievementSections()); state.put("title_views", titleViews());
        return new GameStateView(state);
    }

    public List<Map<String, Object>> actionViews() {
        List<Map<String, Object>> out = new ArrayList<>();
        actions.data().fields().forEachRemaining(entry -> {
            int energy = entry.getValue().path("energy_change").asInt();
            int exp = entry.getValue().path("exp_change").asInt();
            String text = entry.getKey() + String.format(" %+d能量", energy) + (exp != 0 ? " / +" + exp + "经验" : "");
            out.add(map("name", entry.getKey(), "energy_change", energy, "exp_change", exp,
                    "button_text", text, "group", energy > 0 ? "positive" : "negative",
                    "duration_options_source", mapper.convertValue(entry.getValue(), Map.class),
                    "command_payload", map("action_name", entry.getKey())));
        });
        return out;
    }

    public List<Map<String, Object>> dailyViews(List<DailyTask> tasks) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) { DailyTask t = tasks.get(i);
            out.add(map("id", t.id, "name", t.name,
                    "detail_text", String.format("能量 %+d / EXP +%d / %s", t.reward, t.exp, t.done ? "已完成" : "未完成"),
                    "button_text", t.done ? "已完成" : "完成", "button_state", t.done ? "disabled" : "normal",
                    "command_payload", map("index", i)));
        } return out;
    }

    public List<Map<String, Object>> specialViews(List<SpecialTask> tasks) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) { SpecialTask t = tasks.get(i);
            out.add(map("id", t.id, "name", t.name,
                    "detail_text", "EXP +" + t.exp + " / " + (t.done ? "已完成" : "未完成"),
                    "button_text", t.done ? "已完成" : "完成", "button_state", t.done ? "disabled" : "normal",
                    "command_payload", map("index", i)));
        } return out;
    }

    public List<Map<String, Object>> shopViews() {
        List<Map<String, Object>> itemViews = new ArrayList<>();
        for (var item : shop.items()) { String id = str(item.get("id"));
            itemViews.add(map("id", id, "name", item.getOrDefault("name", "未知商品"),
                    "desc", item.getOrDefault("desc", ""), "price", item.getOrDefault("price", 0),
                    "category", item.get("category"), "stock_text", shop.stockText(item),
                    "button_text", shop.buttonText(item), "button_state", shop.canBuy(id) ? "normal" : "disabled",
                    "command_payload", map("item_id", id)));
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        for (String category : shop.categories()) groups.add(map("category", category, "items",
                itemViews.stream().filter(i -> category.equals(i.get("category"))).toList()));
        return groups;
    }

    public List<Map<String, Object>> achievementSections() {
        Set<String> ids = new LinkedHashSet<>(player.unlocked_achievements);
        List<Map<String, Object>> unlocked = new ArrayList<>(), locked = new ArrayList<>();
        for (var achievement : achievements.items()) { var view = achievementEvent(achievement);
            view.put("condition_text", achievementCondition(achievement)); view.put("section",
                    ids.contains(str(achievement.get("id"))) ? "unlocked" : "locked");
            (ids.contains(str(achievement.get("id"))) ? unlocked : locked).add(view);
        }
        return List.of(map("id", "unlocked", "title", "已获取", "empty_text", "暂时还没有获取成就。", "items", unlocked),
                map("id", "locked", "title", "未获取", "empty_text", "所有成就都已获取。", "items", locked));
    }

    public List<Map<String, Object>> titleViews() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var title : titles.items()) { String id = str(title.get("id"));
            boolean unlocked = player.unlocked_titles.contains(id), equipped = id.equals(player.equipped_title);
            var view = titleEvent(title); String status = equipped ? "已佩戴" : unlocked ? "已解锁" : "未解锁";
            view.put("section", unlocked ? "unlocked" : "locked"); view.put("status_text", status);
            view.put("display_text", "【" + view.get("name") + "】 " + status + "\n" + view.get("desc") +
                    "\n条件：" + view.get("condition_text") + "\n效果：" + view.get("bonus_text"));
            view.put("button_text", equipped ? "已佩戴" : unlocked ? "佩戴" : "未解锁");
            view.put("button_state", unlocked && !equipped ? "normal" : "disabled");
            view.put("command_payload", map("title_id", id)); out.add(view);
        } return out;
    }

    public Map<String, Object> achievementEvent(AchievementDefinition achievement) {
        String reward = achievementReward(achievement);
        String condition = achievementCondition(achievement);
        return map("id", achievement.id(), "name", achievement.name(), "desc", achievement.description(),
                "reward_text", reward, "display_text", "【" + achievement.name() + "】\n"
                        + achievement.description() + "\n条件：" + condition + "\n奖励：" + reward);
    }

    private String achievementReward(AchievementDefinition achievement) {
        List<String> rewards = new ArrayList<>();
        if (achievement.rewardCoin() != 0) rewards.add("金币 +" + achievement.rewardCoin());
        if (achievement.rewardExp() != 0) rewards.add("EXP +" + achievement.rewardExp());
        return rewards.isEmpty() ? "无" : String.join(" ", rewards);
    }

    private String achievementCondition(AchievementDefinition achievement) {
        return switch (achievement.conditionType()) {
            case ENERGY_REACH -> "能量达到 " + achievement.targetValue();
            case TASK_DONE_COUNT -> "完成任务 " + achievement.targetValue() + " 次";
            case ACTION_COUNT -> achievement.targetAction() + " " + achievement.targetValue() + " 次";
            case TOTAL_ACTION_COUNT -> "执行任意行动 " + achievement.targetValue() + " 次";
            case SPECIAL_TASK_DONE_COUNT -> "完成特殊任务 " + achievement.targetValue() + " 次";
            case LEVEL -> "达到 Lv." + achievement.targetValue();
            case TASK_COMBO -> "完成指定普通任务与特殊任务组合";
        };
    }
    public Map<String, Object> achievementEvent(Map<String, Object> a) {
        String name = str(a.getOrDefault("name", a.getOrDefault("id", "未知成就")));
        String desc = str(a.getOrDefault("desc", "")), condition = achievementCondition(a), reward = achievementReward(a);
        return map("id", a.get("id"), "name", name, "desc", desc, "reward_text", reward,
                "display_text", "【" + name + "】\n" + desc + "\n条件：" + condition + "\n奖励：" + reward);
    }
    public String achievementReward(Map<String, Object> a) { List<String> r = new ArrayList<>();
        if (num(a.get("reward_coin")) != 0) r.add("金币 +" + num(a.get("reward_coin")));
        if (num(a.get("reward_exp")) != 0) r.add("EXP +" + num(a.get("reward_exp")));
        return r.isEmpty() ? "无" : String.join(" ", r);
    }
    public String achievementCondition(Map<String, Object> a) { String t = str(a.get("condition_type")); Object v = a.get("target_value");
        return switch (t) { case "energy_reach" -> "能量达到 " + v; case "task_done_count" -> "完成任务 " + v + " 次";
            case "action_count" -> str(a.getOrDefault("target_action", "指定行动")) + " " + v + " 次";
            case "total_action_count" -> "执行任意行动 " + v + " 次"; case "special_task_done_count" -> "完成特殊任务 " + v + " 次";
            case "level" -> "达到 Lv." + v; case "task_combo" -> "完成指定普通任务与特殊任务组合"; default -> "未知条件"; };
    }
    public Map<String, Object> titleEvent(Map<String, Object> t) { String name = str(t.getOrDefault("name", t.getOrDefault("id", "未知称号")));
        String desc = str(t.getOrDefault("desc", "")), condition = titles.conditionText(t), bonus = titles.bonusText(t);
        return map("id", t.getOrDefault("id", ""), "name", name, "desc", desc, "condition_text", condition,
                "bonus_text", bonus, "display_text", "【" + name + "】\n" + desc + "\n条件：" + condition + "\n效果：" + bonus);
    }
    public static Map<String, Object> map(Object... values) { Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]); return result; }
    private static int num(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private static String str(Object value) { return Objects.toString(value, ""); }
}
