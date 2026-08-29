package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.domain.DirectionInfo;
import io.github.aomckin.lifehud.domain.SpecialTask;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Links existing daily/special tasks to the Direction hierarchy. Tasks stay fully
 * independent: every link is optional, the deepest selected id infers the levels above,
 * and conflicting manual selections are rejected. Deleting direction nodes clears links.
 */
@Service
public final class DirectionLinkService {
    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;
    private final DreamService dreams;

    public DirectionLinkService(DailyTaskManager dailyTasks, SpecialTaskManager specialTasks, DreamService dreams) {
        this.dailyTasks = dailyTasks; this.specialTasks = specialTasks; this.dreams = dreams;
    }

    /** Links a task by its deepest direction id; upper levels are inferred and validated. */
    public DirectionInfo link(String taskSource, String taskId, String dreamId, String goalId, String dreamMilestoneId) {
        DirectionInfo info = dreams.resolveLink(dreamId, goalId, dreamMilestoneId);
        if ("daily".equalsIgnoreCase(taskSource)) {
            DailyTask task = dailyTask(taskId);
            task.dreamId = info.dreamId() == null ? "" : info.dreamId();
            task.goalId = info.goalId() == null ? "" : info.goalId();
            task.dreamMilestoneId = info.dreamMilestoneId() == null ? "" : info.dreamMilestoneId();
            dailyTasks.save();
            return info;
        }
        if ("special".equalsIgnoreCase(taskSource)) {
            SpecialTask task = specialTask(taskId);
            task.dreamId = info.dreamId() == null ? "" : info.dreamId();
            task.goalId = info.goalId() == null ? "" : info.goalId();
            task.dreamMilestoneId = info.dreamMilestoneId() == null ? "" : info.dreamMilestoneId();
            specialTasks.save();
            return info;
        }
        throw bad("未知的任务来源");
    }

    /** Clears every direction link of a task (unlink = link with all-empty selection). */
    public DirectionInfo unlink(String taskSource, String taskId) { return link(taskSource, taskId, "", "", ""); }

    public DirectionInfo infoOf(String taskSource, String taskId) {
        if ("daily".equalsIgnoreCase(taskSource)) {
            DailyTask task = dailyTask(taskId);
            return dreams.resolve(task.dreamId, task.goalId, task.dreamMilestoneId);
        }
        if ("special".equalsIgnoreCase(taskSource)) {
            SpecialTask task = specialTask(taskId);
            return dreams.resolve(task.dreamId, task.goalId, task.dreamMilestoneId);
        }
        throw bad("未知的任务来源");
    }

    /** Called by DreamService when a direction node is deleted so task links never dangle silently. */
    public void clearReferences(Set<String> dreamIds, Set<String> goalIds, Set<String> milestoneIds) {
        boolean changed = false;
        for (DailyTask task : dailyTasks.allTasks()) {
            if (dreamIds.contains(task.dreamId)) { task.dreamId = ""; changed = true; }
            if (goalIds.contains(task.goalId)) { task.goalId = ""; changed = true; }
            if (milestoneIds.contains(task.dreamMilestoneId)) { task.dreamMilestoneId = ""; changed = true; }
        }
        for (SpecialTask task : specialTasks.allTasks()) {
            if (dreamIds.contains(task.dreamId)) { task.dreamId = ""; changed = true; }
            if (goalIds.contains(task.goalId)) { task.goalId = ""; changed = true; }
            if (milestoneIds.contains(task.dreamMilestoneId)) { task.dreamMilestoneId = ""; changed = true; }
        }
        if (changed) { dailyTasks.save(); specialTasks.save(); }
    }

    private DailyTask dailyTask(String id) {
        return dailyTasks.allTasks().stream().filter(v->v.id.equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "每日任务不存在"));
    }
    private SpecialTask specialTask(String id) {
        return specialTasks.allTasks().stream().filter(v->v.id.equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "特殊任务不存在"));
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
