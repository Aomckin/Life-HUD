package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.RitualExecutionRepository;
import io.github.aomckin.lifehud.repository.RitualRepository;
import io.github.aomckin.lifehud.repository.RitualStepRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rituals are about entering a state, not completing a todo: Ritual + ordered Steps +
 * one Execution per real run. Completing an execution emits RITUAL_COMPLETED exactly
 * once and never touches Growth numbers directly.
 */
@Service
public final class RitualService {
    private final RitualRepository rituals;
    private final RitualStepRepository steps;
    private final RitualExecutionRepository executions;
    private final LifeEventService events;
    private final GrowthCopy copy;

    public RitualService(RitualRepository rituals, RitualStepRepository steps, RitualExecutionRepository executions,
                         LifeEventService events, GrowthCopy copy) {
        this.rituals = rituals; this.steps = steps; this.executions = executions; this.events = events; this.copy = copy;
    }

    public List<Ritual> all() { return rituals.all(); }
    public Ritual get(String id) { return rituals.find(id).orElseThrow(this::missingRitual); }
    public List<RitualStep> stepsOf(String ritualId) { return steps.byRitual(ritualId); }
    public List<RitualExecution> executionsOf(String ritualId) { return executions.byRitual(ritualId); }

    public Map<String, Object> detail(String id) {
        Ritual ritual = get(id);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ritual", ritual);
        map.put("steps", stepsOf(id));
        map.put("executions", executionsOf(id));
        return map;
    }

    public synchronized Ritual create(RitualRequest request) { return save(UUID.randomUUID().toString(), request, null); }

    public synchronized Ritual update(String id, RitualRequest request) {
        get(id);
        return save(id, request, Instant.now());
    }

    /** Disabling keeps history; only enabled rituals show as runnable by default. */
    public synchronized Ritual setEnabled(String id, boolean enabled) {
        Ritual old = get(id);
        Ritual value = new Ritual(old.id(), old.name(), old.description(), old.category(), old.triggerTime(),
                enabled, old.createdAt(), Instant.now());
        rituals.save(value);
        return value;
    }

    public synchronized void delete(String id) {
        get(id);
        rituals.delete(id);
        steps.deleteByRitual(id);
    }

    /** Starts a fresh RUNNING execution carrying a frozen snapshot of the current steps. */
    public synchronized RitualExecution start(String ritualId) {
        Ritual ritual = get(ritualId);
        Instant now = Instant.now();
        List<Map<String, Object>> stepResults = new ArrayList<>();
        for (RitualStep step : stepsOf(ritualId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("stepId", step.id());
            result.put("type", step.type().name());
            result.put("title", step.title());
            result.put("required", step.required());
            result.put("status", "PENDING");
            stepResults.add(result);
        }
        RitualExecution execution = new RitualExecution(UUID.randomUUID().toString(), ritualId, ritual.name(),
                now, null, RitualExecutionStatus.RUNNING, "", stepResults, now, now);
        executions.save(execution);
        return execution;
    }

    /** Completes a RUNNING execution; repeated complete calls return it without a second LifeEvent. */
    public synchronized RitualExecution complete(String id, String note) {
        RitualExecution old = execution(id);
        if (old.status() == RitualExecutionStatus.COMPLETED) return old;
        if (old.status() != RitualExecutionStatus.RUNNING) throw bad("该执行已结束，不能完成");
        Instant now = Instant.now();
        RitualExecution value = new RitualExecution(old.id(), old.ritualId(), old.ritualName(), old.startedAt(),
                now, RitualExecutionStatus.COMPLETED, clean(note), old.stepResults(), old.createdAt(), now);
        executions.save(value);
        events.record(LifeEventType.RITUAL_COMPLETED, "ritual", value.ritualName(),
                copy.ritualCompletedDescription(value.ritualName()), List.of("ritual"),
                Map.of("ritualId", value.ritualId(), "executionId", value.id(),
                        "durationSeconds", Duration.between(value.startedAt(), now).getSeconds(), "sourceId", value.id()));
        return value;
    }

    public synchronized RitualExecution cancel(String id, String note) {
        RitualExecution old = execution(id);
        if (old.status() != RitualExecutionStatus.RUNNING) return old;
        RitualExecution value = new RitualExecution(old.id(), old.ritualId(), old.ritualName(), old.startedAt(),
                Instant.now(), RitualExecutionStatus.CANCELLED, clean(note), old.stepResults(), old.createdAt(), Instant.now());
        executions.save(value);
        return value;
    }

    /** Marks one step result inside a RUNNING execution (CHECK done, NOTE text, TIMER elapsed...). */
    public synchronized RitualExecution updateStep(String executionId, String stepId, boolean done, String noteValue) {
        RitualExecution old = execution(executionId);
        if (old.status() != RitualExecutionStatus.RUNNING) throw bad("该执行已结束");
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> result : old.stepResults()) {
            if (stepId.equals(String.valueOf(result.get("stepId")))) {
                Map<String, Object> updated = new LinkedHashMap<>(result);
                updated.put("status", done ? "DONE" : "PENDING");
                if (noteValue != null) updated.put("note", noteValue);
                results.add(updated);
            } else results.add(result);
        }
        RitualExecution value = new RitualExecution(old.id(), old.ritualId(), old.ritualName(), old.startedAt(),
                null, old.status(), old.note(), results, old.createdAt(), Instant.now());
        executions.save(value);
        return value;
    }

    public record ExecutionView(RitualExecution execution, List<RitualStep> steps) { }

    public ExecutionView executionView(String id) {
        RitualExecution execution = execution(id);
        return new ExecutionView(execution, stepsOf(execution.ritualId()));
    }

    private Ritual save(String id, RitualRequest request, Instant updatedOverride) {
        String name = clean(request == null ? null : request.name());
        if (name.isBlank()) throw bad("仪式名称不能为空");
        Ritual old = rituals.find(id).orElse(null);
        Instant now = Instant.now();
        Ritual value = new Ritual(id, name, clean(request.description()), clean(request.category()),
                request.triggerTime(), request == null || request.enabled(), old == null ? now : old.createdAt(), now);
        rituals.save(value);
        List<RitualStep> next = new ArrayList<>();
        List<RitualRequest.RitualStepInput> inputs = request.steps() == null ? List.of() : request.steps();
        int order = 0;
        for (RitualRequest.RitualStepInput input : inputs) {
            String title = clean(input.title());
            if (title.isBlank()) continue;
            int sortOrder = input.sortOrder() == null ? order : input.sortOrder();
            next.add(new RitualStep(UUID.randomUUID().toString(), id,
                    input.type() == null ? RitualStepType.TEXT : input.type(), title, clean(input.content()),
                    input.durationSeconds() == null ? 0 : Math.max(0, input.durationSeconds()),
                    clean(input.url()), sortOrder, input.required() == null || input.required()));
            order = sortOrder + 1;
        }
        steps.replaceRitualSteps(id, next);
        return value;
    }

    private RitualExecution execution(String id) { return executions.find(id).orElseThrow(this::missingExecution); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missingRitual() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "仪式不存在"); }
    private ResponseStatusException missingExecution() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "仪式执行不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
