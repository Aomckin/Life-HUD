package io.github.aomckin.lifehud;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.*;
import io.github.aomckin.lifehud.service.*;
import org.springframework.beans.factory.ObjectProvider;

/** Shared wiring for v0.5 Direction / Ritual / Now service tests. */
public final class V05TestWiring {
    public final TestDataSupport data;
    public final Player player;
    public final LifeEventService events;
    public final LifeEventRepository lifeEvents;
    public final GrowthEngine engine;
    public final GrowthRecordRepository records;
    public final DreamService dreams;
    public final DirectionLinkService links;
    public final RitualService rituals;
    public final NowService now;
    public final DreamRepository dreamRepo;
    public final GoalRepository goalRepo;
    public final DreamMilestoneRepository milestoneRepo;
    public final RitualRepository ritualRepo;
    public final RitualStepRepository stepRepo;
    public final RitualExecutionRepository executionRepo;
    public final NowRepository nowRepo;
    public final ImageStorageService images;
    public final DailyTaskManager dailyTasks;
    public final TaskService taskService;
    public final SpecialTaskManager specialTasks;

    private V05TestWiring(TestDataSupport data, Player player, LifeEventService events, LifeEventRepository lifeEvents,
                          GrowthEngine engine, GrowthRecordRepository records, DreamService dreams,
                          DirectionLinkService links, RitualService rituals, NowService now, DreamRepository dreamRepo,
                          GoalRepository goalRepo, DreamMilestoneRepository milestoneRepo, RitualRepository ritualRepo,
                          RitualStepRepository stepRepo, RitualExecutionRepository executionRepo, NowRepository nowRepo,
                          ImageStorageService images, DailyTaskManager dailyTasks, SpecialTaskManager specialTasks,
                          TaskService taskService) {
        this.data = data; this.player = player; this.events = events; this.lifeEvents = lifeEvents;
        this.engine = engine; this.records = records; this.dreams = dreams; this.links = links;
        this.rituals = rituals; this.now = now; this.dreamRepo = dreamRepo; this.goalRepo = goalRepo;
        this.milestoneRepo = milestoneRepo; this.ritualRepo = ritualRepo; this.stepRepo = stepRepo;
        this.executionRepo = executionRepo; this.nowRepo = nowRepo; this.images = images;
        this.dailyTasks = dailyTasks; this.specialTasks = specialTasks; this.taskService = taskService;
    }

    public static V05TestWiring create(TestDataSupport data) {
        var player = data.players.load(100, 180);
        var levels = new LevelService(data.json.read("level.json"));
        var playerService = new PlayerService();
        var lifeRepo = new LifeEventRepository(data.json, data.mapper);
        var records = new GrowthRecordRepository(data.json, data.mapper);
        var focus = new FocusSessionRepository(data.json, data.mapper);
        var milestones = new MilestoneRepository(data.json, data.mapper);
        var stats = new GrowthStatsService(new GrowthStatsRepository(data.json, data.mapper), focus, lifeRepo, milestones);
        var copy = new GrowthCopy(data.json);
        var catalog = new GrowthCatalog(data.json, data.mapper);
        var gameConfig = new GameConfig((ObjectNode) data.json.read("config.json"));
        final GrowthEngine[] holder = new GrowthEngine[1];
        var events = new LifeEventService(lifeRepo, new ObjectProvider<GrowthEngine>() {
            @Override public GrowthEngine getObject() { return holder[0]; }
            @Override public GrowthEngine getObject(Object... args) { return holder[0]; }
            @Override public GrowthEngine getIfAvailable() { return holder[0]; }
            @Override public GrowthEngine getIfUnique() { return holder[0]; }
        });
        var engine = new GrowthEngine(new GrowthRules(new GrowthEconomy(data.json), copy, gameConfig), records,
                player, playerService, data.players, levels, events, stats, catalog, copy,
                new AchievementEvaluator(lifeRepo,new MediaRepository(data.json,data.mapper),new LifeDateService()));
        holder[0] = engine;
        var dreamRepo = new DreamRepository(data.json, data.mapper);
        var goalRepo = new GoalRepository(data.json, data.mapper);
        var milestoneRepo = new DreamMilestoneRepository(data.json, data.mapper);
        final DirectionLinkService[] linkHolder = new DirectionLinkService[1];
        var dreamService = new DreamService(dreamRepo, goalRepo, milestoneRepo, events, copy,
                new ObjectProvider<DirectionLinkService>() {
                    @Override public DirectionLinkService getObject() { return linkHolder[0]; }
                    @Override public DirectionLinkService getObject(Object... args) { return linkHolder[0]; }
                    @Override public DirectionLinkService getIfAvailable() { return linkHolder[0]; }
                    @Override public DirectionLinkService getIfUnique() { return linkHolder[0]; }
                });
        var dailyTasks = new DailyTaskManager(data.json);
        var specialTasks = new SpecialTaskManager(data.json, player.special_task_slots);
        var links = new DirectionLinkService(dailyTasks, specialTasks, dreamService);
        linkHolder[0] = links;
        var ritualRepo = new RitualRepository(data.json, data.mapper);
        var stepRepo = new RitualStepRepository(data.json, data.mapper);
        var executionRepo = new RitualExecutionRepository(data.json, data.mapper);
        var rituals = new RitualService(ritualRepo, stepRepo, executionRepo, events, copy);
        var nowRepo = new NowRepository(data.json, data.mapper);
        var uploadStorage=new UploadStorageService(data.json,data.mapper);
        var images = new ImageStorageService(data.json,uploadStorage);
        var now = new NowService(nowRepo, dreamRepo, goalRepo, events, copy, new AudioStorageService(data.json,uploadStorage), images);
        var taskService = new TaskService(dailyTasks, specialTasks, data.logs, null, events, copy);
        return new V05TestWiring(data, player, events, lifeRepo, engine, records, dreamService, links, rituals, now,
                dreamRepo, goalRepo, milestoneRepo, ritualRepo, stepRepo, executionRepo, nowRepo, images, dailyTasks,
                specialTasks, taskService);
    }
}
