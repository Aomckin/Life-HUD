package io.github.aomckin.lifehud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.core.ActionCatalog;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.core.GameCore;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.JsonRepository;
import io.github.aomckin.lifehud.repository.LogRepository;
import io.github.aomckin.lifehud.repository.LifeEventRepository;
import io.github.aomckin.lifehud.repository.GrowthRecordRepository;
import io.github.aomckin.lifehud.repository.GrowthStatsRepository;
import io.github.aomckin.lifehud.repository.FocusSessionRepository;
import io.github.aomckin.lifehud.repository.MilestoneRepository;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import io.github.aomckin.lifehud.service.*;
import java.util.List;
import java.util.Map;

/** Test-only composition root; production composition is GameConfiguration. */
public final class TestGameContext extends GameCore {
    private final JsonRepository json; private final PlayerRepository players; private final ObjectMapper mapper;
    private final GameConfig config; private final ActionCatalog actions; private final Player player;
    private final DailyTaskManager daily; private final SpecialTaskManager special; private final LevelService levels;
    private final TitleSystem titles; private final AchievementSystem achievements; private final ShopManager shop;
    private final GameViewAssembler views; private final AchievementService achievementService;

    private TestGameContext(JsonRepository json, PlayerRepository players, LogRepository logs, ObjectMapper mapper,
                            GameConfig config, ActionCatalog actions, Player player, DailyTaskManager daily,
                            SpecialTaskManager special, LevelService levels, TitleSystem titles,
                            AchievementSystem achievements, ShopManager shop, GameViewAssembler views,
                            GameQueryService queries, AchievementService achievementService,
                            ActionService action, TaskService task, ShopService shopService, TitleService title,
                            ProgressionService progression, GrowthCopy copy) {
        super(action, task, shopService, title, progression, queries, player, logs, copy);
        this.json=json; this.players=players; this.mapper=mapper; this.config=config; this.actions=actions;
        this.player=player; this.daily=daily; this.special=special; this.levels=levels; this.titles=titles;
        this.achievements=achievements; this.shop=shop; this.views=views; this.achievementService=achievementService;
    }

    public static TestGameContext create(JsonRepository json, PlayerRepository players, LogRepository logs, ObjectMapper mapper) {
        GameConfig config = new GameConfig((ObjectNode) json.read("config.json"));
        ActionCatalog actions = new ActionCatalog(json); Player player = players.load(config.defaultEnergy(), config.maxEnergy());
        DailyTaskManager daily = new DailyTaskManager(json); SpecialTaskManager special = new SpecialTaskManager(json, player.special_task_slots);
        LevelService levels = new LevelService(json.read("level.json")); PlayerService playerService = new PlayerService();
        TitleSystem titles = new TitleSystem(player, players, json, mapper, playerService);
        AchievementSystem achievements = new AchievementSystem(player, levels, json, mapper);
        ShopManager shop = new ShopManager(player, players, json, mapper, playerService);
        GameViewAssembler views = new GameViewAssembler(config, actions, player, mapper, levels, logs, daily, special, shop, achievements, titles);
        GameQueryService queries = new GameQueryService(actions, views);
        AchievementService achievementService = new AchievementService(player, achievements, titles, daily, playerService, players, views);
        ProgressionService progression = new ProgressionService(achievementService, levels, queries, views);
        ActionService action = new ActionService(player, actions, players, playerService, titles, progression, logs, queries);
        LifeEventRepository lifeEventRepository = new LifeEventRepository(json, mapper);
        GrowthRecordRepository growthRecords = new GrowthRecordRepository(json, mapper);
        FocusSessionRepository focusSessions = new FocusSessionRepository(json, mapper);
        MilestoneRepository milestones = new MilestoneRepository(json, mapper);
        GrowthStatsService growthStats = new GrowthStatsService(new GrowthStatsRepository(json, mapper), focusSessions,
                lifeEventRepository, milestones);
        // Wire the same event→engine auto-settlement the Spring context provides via ObjectProvider.
        final GrowthEngine[] engineHolder = new GrowthEngine[1];
        LifeEventService lifeEvents = new LifeEventService(lifeEventRepository, new org.springframework.beans.factory.ObjectProvider<GrowthEngine>() {
            @Override public GrowthEngine getObject() { return engineHolder[0]; }
            @Override public GrowthEngine getObject(Object... args) { return engineHolder[0]; }
            @Override public GrowthEngine getIfAvailable() { return engineHolder[0]; }
            @Override public GrowthEngine getIfUnique() { return engineHolder[0]; }
        });
        GrowthCopy growthCopy = new GrowthCopy(json);
        GrowthCatalog growthCatalog = new GrowthCatalog(json, mapper);
        GrowthEngine growth = new GrowthEngine(new GrowthRules(new GrowthEconomy(json), growthCopy, config),
                growthRecords, player, playerService, players, levels, lifeEvents, growthStats, growthCatalog, growthCopy);
        engineHolder[0] = growth;
        TaskService task = new TaskService(daily, special, logs, queries, lifeEvents, growthCopy);
        ShopService shopService = new ShopService(shop, daily, special, logs, queries); TitleService title = new TitleService(titles, queries);
        return new TestGameContext(json, players, logs, mapper, config, actions, player, daily, special, levels, titles,
                achievements, shop, views, queries, achievementService, action, task, shopService, title, progression, growthCopy);
    }

    public JsonRepository json() { return json; } public PlayerRepository players() { return players; }
    public ObjectMapper mapper() { return mapper; } public ObjectNode config() { return config.data(); }
    public ObjectNode actions() { return actions.data(); } public Player player() { return player; }
    public DailyTaskManager dailyTasks() { return daily; } public SpecialTaskManager specialTasks() { return special; }
    public LevelService levels() { return levels; } public TitleSystem titles() { return titles; }
    public AchievementSystem achievements() { return achievements; } public ShopManager shop() { return shop; }
    public GameViewAssembler views() { return views; }
    public List<Map<String, Object>> checkAchievements() { return achievementService.checkAchievements().stream().map(views::achievementEvent).toList(); }
}
