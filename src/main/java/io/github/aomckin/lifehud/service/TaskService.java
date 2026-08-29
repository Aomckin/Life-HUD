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
        DailyTask task = dailyTasks.tasks().get(index);
        int[] reward = dailyTasks.finish(index);
        if (reward[0] > 0 || reward[1] > 0) {
            // baseExp is deprecated: GrowthRules only reads baseEnergy, EXP comes solely from Energy SPEND.
            events.record(LifeEventType.TASK_COMPLETED,"task",task.name,"完成每日任务",List.of("task"),
                    Map.of("taskId",task.id,"taskSource","daily","baseEnergy",reward[0],
                            "dreamId",task.dreamId,"goalId",task.goalId,"dreamMilestoneId",task.dreamMilestoneId));
            logs.action(copy.taskCompletedLog(task.name), copy.growthSyncedLog(), 0);
        }
        return new OperationResult(true,"任务完成",List.of(new GameEvent(GameEvents.TASK_COMPLETE,
                GameViewAssembler.map("task",task.toMap(),"source","daily"))),queries.state());
    }

    public OperationResult completeSpecial(int index) {
        if (index < 0 || index >= specialTasks.tasks().size()) return queries.error("特殊任务不存在");
        SpecialTask task = specialTasks.tasks().get(index);
        int[] reward = specialTasks.finish(index);
        if (reward[0] > 0 || reward[1] > 0) {
            // Special tasks had no Energy field; their legacy exp value becomes the Energy reward (capped by growth.json).
            events.record(LifeEventType.TASK_COMPLETED,"task",task.name,"完成特殊任务",List.of("task"),
                    Map.of("taskId",task.id,"taskSource","special","baseEnergy",task.exp,
                            "dreamId",task.dreamId,"goalId",task.goalId,"dreamMilestoneId",task.dreamMilestoneId));
            logs.action(copy.specialTaskCompletedLog(task.name), copy.growthSyncedLog(), 0);
        }
        return new OperationResult(true,"特殊任务完成",List.of(new GameEvent(GameEvents.TASK_COMPLETE,
                GameViewAssembler.map("task",task.toMap(),"source","special"))),queries.state());
    }

    public OperationResult refresh() {
        dailyTasks.redraw(); return new OperationResult(true, "每日任务已刷新", List.of(), queries.state());
    }

}
