package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.domain.DirectionInfo;
import io.github.aomckin.lifehud.domain.SpecialTask;
import io.github.aomckin.lifehud.service.DailyTaskManager;
import io.github.aomckin.lifehud.service.DirectionLinkService;
import io.github.aomckin.lifehud.service.SpecialTaskManager;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Task pool management (v0.5.3.1): definitions vs today instances.
 * /api/task-directions serves only TODAY's active instances; this controller
 * manages the full definition pool (add / edit / delete / disable).
 */
@RestController @RequestMapping("/api/task-pool")
public final class TaskPoolController {
    public record DefinitionRequest(String name, Integer energy, Integer exp) { }
    public record EnabledRequest(boolean enabled) { }

    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;
    private final DirectionLinkService links;

    public TaskPoolController(DailyTaskManager dailyTasks, SpecialTaskManager specialTasks, DirectionLinkService links) {
        this.dailyTasks = dailyTasks; this.specialTasks = specialTasks; this.links = links;
    }

    @GetMapping
    public Map<String,Object> pool() {
        Map<String,Object> map = new LinkedHashMap<>();
        List<Map<String,Object>> daily = new ArrayList<>();
        dailyTasks.allTasks().forEach(t -> daily.add(definition("daily", t.id, t.name, t.reward, t.exp,
                t.enabled, links.infoOf("daily", t.id))));
        List<Map<String,Object>> special = new ArrayList<>();
        specialTasks.allTasks().forEach(t -> special.add(definition("special", t.id, t.name, 0, t.exp,
                t.enabled, links.infoOf("special", t.id))));
        map.put("daily", daily);
        map.put("special", special);
        return map;
    }

    @PostMapping("/daily") @ResponseStatus(HttpStatus.CREATED)
    public DailyTask createDaily(@RequestBody DefinitionRequest request) {
        requireName(request);
        return dailyTasks.addDefinition(request.name().trim(), orZero(request.energy()), orZero(request.exp()));
    }
    @PutMapping("/daily/{id}")
    public DailyTask updateDaily(@PathVariable String id, @RequestBody DefinitionRequest request) {
        requireName(request);
        return dailyTasks.updateDefinition(id, request.name().trim(), orZero(request.energy()), orZero(request.exp()));
    }
    @DeleteMapping("/daily/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDaily(@PathVariable String id) { dailyTasks.removeDefinition(id); }
    @PostMapping("/daily/{id}/enabled")
    public DailyTask setDailyEnabled(@PathVariable String id, @RequestBody EnabledRequest request) {
        dailyTasks.setEnabled(id, request.enabled());
        return dailyTasks.allTasks().stream().filter(v -> v.id.equals(id)).findFirst().orElseThrow();
    }

    @PostMapping("/special") @ResponseStatus(HttpStatus.CREATED)
    public SpecialTask createSpecial(@RequestBody DefinitionRequest request) {
        requireName(request);
        return specialTasks.addDefinition(request.name().trim(), orZero(request.exp()));
    }
    @PutMapping("/special/{id}")
    public SpecialTask updateSpecial(@PathVariable String id, @RequestBody DefinitionRequest request) {
        requireName(request);
        return specialTasks.updateDefinition(id, request.name().trim(), orZero(request.exp()));
    }
    @DeleteMapping("/special/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpecial(@PathVariable String id) { specialTasks.removeDefinition(id); }
    @PostMapping("/special/{id}/enabled")
    public SpecialTask setSpecialEnabled(@PathVariable String id, @RequestBody EnabledRequest request) {
        specialTasks.setEnabled(id, request.enabled());
        return specialTasks.allTasks().stream().filter(v -> v.id.equals(id)).findFirst().orElseThrow();
    }

    private Map<String,Object> definition(String source, String id, String name, int energy, int exp,
                                          boolean enabled, DirectionInfo direction) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("source", source); map.put("taskId", id); map.put("name", name);
        map.put("energy", energy); map.put("exp", exp); map.put("enabled", enabled);
        map.put("direction", direction);
        return map;
    }
    private void requireName(DefinitionRequest request) {
        if (request == null || request.name() == null || request.name().isBlank())
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "任务名称不能为空");
    }
    private int orZero(Integer value) { return value == null ? 0 : Math.max(0, value); }
}
