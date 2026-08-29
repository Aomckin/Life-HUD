package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.DirectionInfo;
import io.github.aomckin.lifehud.service.DailyTaskManager;
import io.github.aomckin.lifehud.service.DirectionLinkService;
import io.github.aomckin.lifehud.service.SpecialTaskManager;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** Task Direction links for the existing daily / special task stores. */
@RestController @RequestMapping("/api/task-directions")
public final class DirectionLinkController {
    private final DirectionLinkService links;
    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;

    public DirectionLinkController(DirectionLinkService links, DailyTaskManager dailyTasks, SpecialTaskManager specialTasks) {
        this.links = links; this.dailyTasks = dailyTasks; this.specialTasks = specialTasks;
    }

    public record LinkRequest(String dreamId, String goalId, String dreamMilestoneId) { }

    @GetMapping
    public Map<String,Object> all() {
        Map<String,Object> map = new LinkedHashMap<>();
        List<Map<String,Object>> daily = new ArrayList<>();
        dailyTasks.allTasks().forEach(task -> daily.add(view("daily", task.id, task.name, task.done,
                links.infoOf("daily", task.id))));
        List<Map<String,Object>> special = new ArrayList<>();
        specialTasks.allTasks().forEach(task -> special.add(view("special", task.id, task.name, task.done,
                links.infoOf("special", task.id))));
        map.put("daily", daily);
        map.put("special", special);
        return map;
    }

    @PutMapping("/{source}/{taskId}")
    public DirectionInfo link(@PathVariable String source, @PathVariable String taskId, @RequestBody LinkRequest request) {
        return links.link(source, taskId, request.dreamId(), request.goalId(), request.dreamMilestoneId());
    }

    @DeleteMapping("/{source}/{taskId}")
    public DirectionInfo unlink(@PathVariable String source, @PathVariable String taskId) {
        return links.unlink(source, taskId);
    }

    private Map<String,Object> view(String source, String id, String name, boolean done, DirectionInfo info) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("source", source); map.put("taskId", id); map.put("name", name); map.put("done", done);
        map.put("direction", info);
        return map;
    }
}
