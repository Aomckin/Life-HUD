package io.github.aomckin.lifehud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.core.ActionCatalog;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.AchievementRepository;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import io.github.aomckin.lifehud.repository.LogRepository;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import io.github.aomckin.lifehud.service.AchievementSystem;
import io.github.aomckin.lifehud.service.DailyTaskManager;
import io.github.aomckin.lifehud.service.GameQueryService;
import io.github.aomckin.lifehud.service.GameViewAssembler;
import io.github.aomckin.lifehud.service.LevelService;
import io.github.aomckin.lifehud.service.PlayerService;
import io.github.aomckin.lifehud.service.ShopManager;
import io.github.aomckin.lifehud.service.SpecialTaskManager;
import io.github.aomckin.lifehud.service.TitleSystem;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for runtime state and read models. */
@Configuration
public class GameConfiguration {
    @Bean public Clock clock() { return Clock.systemDefaultZone(); }
    @Bean public GameConfig gameConfig(JsonFileStore files) {
        return new GameConfig((com.fasterxml.jackson.databind.node.ObjectNode) files.read("config.json"));
    }
    @Bean public ActionCatalog actionCatalog(JsonFileStore files) { return new ActionCatalog(files); }
    @Bean public Player player(PlayerRepository players, GameConfig config) {
        return players.load(config.defaultEnergy(), config.maxEnergy());
    }
    @Bean public DailyTaskManager dailyTaskManager(JsonFileStore files) { return new DailyTaskManager(files); }
    @Bean public SpecialTaskManager specialTaskManager(JsonFileStore files, Player player) {
        return new SpecialTaskManager(files, player.special_task_slots);
    }
    @Bean public LevelService levelService(JsonFileStore files) { return new LevelService(files.read("level.json")); }
    @Bean public TitleSystem titleSystem(Player player, PlayerRepository players, JsonFileStore files,
                                         ObjectMapper mapper, PlayerService playerService) {
        return new TitleSystem(player, players, files, mapper, playerService);
    }
    @Bean public AchievementSystem achievementSystem(Player player, LevelService levels,
                                                     AchievementRepository achievements, JsonFileStore files) {
        return new AchievementSystem(player, levels, achievements, files);
    }
    @Bean public ShopManager shopManager(Player player, PlayerRepository players, JsonFileStore files,
                                         ObjectMapper mapper, PlayerService playerService) {
        return new ShopManager(player, players, files, mapper, playerService);
    }
    @Bean public GameViewAssembler gameViewAssembler(GameConfig config, ActionCatalog actions, Player player,
                                                     ObjectMapper mapper, LevelService levels, LogRepository logs,
                                                     DailyTaskManager daily, SpecialTaskManager special,
                                                     ShopManager shop, AchievementSystem achievements, TitleSystem titles) {
        return new GameViewAssembler(config, actions, player, mapper, levels, logs, daily, special,
                shop, achievements, titles);
    }
    @Bean public GameQueryService gameQueryService(ActionCatalog actions, GameViewAssembler assembler) {
        return new GameQueryService(actions, assembler);
    }
}
