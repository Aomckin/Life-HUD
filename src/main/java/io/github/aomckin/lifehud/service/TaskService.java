package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.SpecialTask;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import io.github.aomckin.lifehud.repository.LogRepository;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class TaskService {
    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;
    private final LogRepository logs;
    private final GameQueryService queries;
    private final LifeEventService events;
    private final GrowthCopy copy;

    public TaskService(DailyTaskManager dailyTasks, SpecialTaskManager specialTasks,
                       LogRepository logs, GameQueryService queries, LifeEventService events, GrowthCopy copy) {
        this.dailyTasks = dailyTasks; this.specialTasks = specialTasks;
        this.logs = logs; this.queries = queries; this.events = events; this.copy = copy;
    }

    public OperationResult completeDaily(int index) {
        if (index < 0 || index >= dailyTasks.tasks().size()) return queries.error("任务不存在");
        return finishDaily(dailyTasks.tasks().get(index));
    }

    /** Action-desk completion by task id; works for any daily task and is idempotent. */
    public OperationResult completeDailyById(String id) {
        DailyTask task = dailyTasks.allTasks().stream().filter(v -> v.id.equals(id)).findFirst().orElse(null);
        if (task == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "任务不存在");
        return finishDaily(task);
    }

    private OperationResult finishDaily(DailyTask task) {
        int[] reward = dailyTasks.finishById(task.id);
        if (reward[0] > 0 || reward[1] > 0) {
            // baseExp is deprecated: GrowthRules only reads baseEnergy, EXP comes solely from Energy SPEND.
            events.record(LifeEventType.TASK_COMPLETED,"task",task.name,"完成每日任务",List.of("task"),
                    Map.of("taskId",task.id,"taskSource","daily","baseEnergy",reward[0],
                            "dreamId",task.dreamId,"goalId",task.goalId,"dreamMilestoneId",task.dreamMilestoneId));
            logs.action(copy.taskCompletedLog(task.name), copy.growthSyncedLog(), 0);
        }
        return new OperationResult(true, task.done ? "任务完成" : "任务已完成", List.of(new GameEvent(GameEvents.TASK_COMPLETE,
                GameViewAssembler.map("task",task.toMap(),"source","daily"))),
                queries == null ? Map.of() : queries.state());
    }

    public OperationResult completeSpecial(int index) {
        if (index < 0 || index >= specialTasks.tasks().size()) return queries.error("特殊任务不存在");
        return finishSpecial(specialTasks.tasks().get(index));
    }

    /** Action-desk completion by task id; works for any special task and is idempotent. */
    public OperationResult completeSpecialById(String id) {
        SpecialTask task = specialTasks.allTasks().stream().filter(v -> v.id.equals(id)).findFirst().orElse(null);
        if (task == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "特殊任务不存在");
        return finishSpecial(task);
    }

    private OperationResult finishSpecial(SpecialTask task) {
        int[] reward = specialTasks.finishById(task.id);
        if (reward[0] > 0 || reward[1] > 0) {
            // Special tasks grant EXP directly; legacy Energy-shaped values are migrated once by SpecialTaskManager.
            events.record(LifeEventType.TASK_COMPLETED,"task",task.name,"完成特殊任务",List.of("task"),
                    Map.of("taskId",task.id,"taskSource","special","baseExp",task.exp,
                            "dreamId",task.dreamId,"goalId",task.goalId,"dreamMilestoneId",task.dreamMilestoneId));
            logs.action(copy.specialTaskCompletedLog(task.name), copy.growthSyncedLog(), 0);
        }
        return new OperationResult(true,"特殊任务完成",List.of(new GameEvent(GameEvents.TASK_COMPLETE,
                GameViewAssembler.map("task",task.toMap(),"source","special"))),
                queries == null ? Map.of() : queries.state());
    }

    public OperationResult refresh() {
        dailyTasks.redraw(); return new OperationResult(true, "每日任务已刷新", List.of(), queries.state());
    }

}
