package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.AchievementDefinition;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Applies unlock rewards and converts progression discoveries to event payloads. */
@Service
public final class AchievementService {
    private final Player player;
    private final AchievementSystem achievements;
    private final TitleSystem titles;
    private final DailyTaskManager dailyTasks;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final GameViewAssembler views;

    public AchievementService(Player player, AchievementSystem achievements, TitleSystem titles,
                              DailyTaskManager dailyTasks, PlayerService playerService,
                              PlayerRepository playerRepository, GameViewAssembler views) {
        this.player = player; this.achievements = achievements; this.titles = titles;
        this.dailyTasks = dailyTasks; this.playerService = playerService;
        this.playerRepository = playerRepository; this.views = views;
    }

    public List<AchievementDefinition> checkAchievements() {
        List<AchievementDefinition> unlocked = achievements.checkDefinitions();
        for (AchievementDefinition achievement : unlocked) {
            playerService.unlockAchievement(player, achievement.id());
            playerService.addCoin(player, achievement.rewardCoin());
            // v0.4: EXP only comes from actually spent Energy settled by GrowthEngine; achievements never grant it.
        }
        if (!unlocked.isEmpty()) {
            playerRepository.save(player);
        }
        return unlocked;
    }
    public List<Map<String, Object>> checkTitles() {
        List<Map<String, Object>> unlocked = titles.checkTitles(dailyTasks.allTasks());
        boolean autoEquip = player.equipped_title.isBlank();
        for (var title : unlocked) {
            String id = Objects.toString(title.get("id"), "");
            playerService.unlockTitle(player, id);
            if (autoEquip && !id.isBlank()) { playerService.equipTitle(player, id); autoEquip = false; }
        }
        if (!unlocked.isEmpty()) playerRepository.save(player);
        return unlocked.stream().map(views::titleEvent).toList();
    }

    public int currentExp() { return player.exp; }
    private int num(Object value) { return value instanceof Number n ? n.intValue() : 0; }
}
