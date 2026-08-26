package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.domain.SpecialTask;
import io.github.aomckin.lifehud.domain.TaskSource;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import io.github.aomckin.lifehud.repository.LogRepository;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class TaskService {
    private final Player player;
    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final TitleSystem titles;
    private final ShopManager shop;
    private final LogRepository logs;
    private final ProgressionService progression;
    private final GameQueryService queries;

    public TaskService(Player player, DailyTaskManager dailyTasks, SpecialTaskManager specialTasks,
                       PlayerService playerService, PlayerRepository playerRepository, TitleSystem titles,
                       ShopManager shop, LogRepository logs, ProgressionService progression,
                       GameQueryService queries) {
        this.player = player; this.dailyTasks = dailyTasks; this.specialTasks = specialTasks;
        this.playerService = playerService; this.playerRepository = playerRepository; this.titles = titles;
        this.shop = shop; this.logs = logs; this.progression = progression; this.queries = queries;
    }

    public OperationResult completeDaily(int index) {
        if (index < 0 || index >= dailyTasks.tasks().size()) return queries.error("任务不存在");
        DailyTask task = dailyTasks.tasks().get(index); int before = player.exp;
        int[] reward = dailyTasks.finish(index);
        if (reward[0] > 0 || reward[1] > 0) {
            int energy = titles.applyEnergyBonus(reward[0]);
            int exp = applyExp(reward[1], task.id, TaskSource.DAILY);
            playerService.addDailyTaskReward(player, energy, exp); playerRepository.save(player);
            logs.action("完成任务：" + task.name, "能量+" + energy + " 经验+" + exp, player.energy);
        }
        return progression.result(true, "任务完成", before, List.of(new GameEvent(
                GameEvents.TASK_COMPLETE, GameViewAssembler.map("task", task.toMap(), "source", "daily"))));
    }

    public OperationResult completeSpecial(int index) {
        if (index < 0 || index >= specialTasks.tasks().size()) return queries.error("特殊任务不存在");
        SpecialTask task = specialTasks.tasks().get(index); int before = player.exp;
        int[] reward = specialTasks.finish(index);
        if (reward[0] > 0 || reward[1] > 0) {
            int exp = applyExp(reward[1], task.id, TaskSource.SPECIAL);
            playerService.addSpecialTaskReward(player, reward[0], exp); playerRepository.save(player);
            logs.action("完成特殊任务：" + task.name, "金币+" + reward[0] + " 经验+" + exp, player.energy);
        }
        return progression.result(true, "特殊任务完成", before, List.of(new GameEvent(
                GameEvents.TASK_COMPLETE, GameViewAssembler.map("task", task.toMap(), "source", "special"))));
    }

    public OperationResult refresh() {
        dailyTasks.redraw(); return new OperationResult(true, "每日任务已刷新", List.of(), queries.state());
    }

    public int applyExp(int exp, String id, TaskSource source) {
        return titles.applyTaskExpBonus(exp, id, source) * shop.taskExpMultiplier();
    }
}
