package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.DreamMilestoneRepository;
import io.github.aomckin.lifehud.repository.DreamRepository;
import io.github.aomckin.lifehud.repository.GoalRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Direction aggregate: Dream → Goal → DreamMilestone. It only produces business facts;
 * Growth consumes the emitted LifeEvents. Completion is idempotent — a repeated complete
 * request must never emit a second event. Archiving is the preferred soft deletion.
 */
@Service
public final class DreamService {
    private final DreamRepository dreams;
    private final GoalRepository goals;
    private final DreamMilestoneRepository milestones;
    private final LifeEventService events;
    private final GrowthCopy copy;
    private final org.springframework.beans.factory.ObjectProvider<DirectionLinkService> links;

    public DreamService(DreamRepository dreams, GoalRepository goals, DreamMilestoneRepository milestones,
                        LifeEventService events, GrowthCopy copy,
                        org.springframework.beans.factory.ObjectProvider<DirectionLinkService> links) {
        this.dreams = dreams; this.goals = goals; this.milestones = milestones; this.events = events;
        this.copy = copy; this.links = links;
    }

    public List<Dream> all() { return dreams.all(); }
    public Dream get(String id) { return dreams.find(id).orElseThrow(this::missingDream); }
    public List<Goal> goalsOf(String dreamId) { return goals.byDream(dreamId); }
    public List<DreamMilestone> milestonesOf(String goalId) { return milestones.byGoal(goalId); }

    public synchronized Dream create(DreamRequest request) {
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("梦想标题不能为空");
        Instant now = Instant.now();
        Dream value = new Dream(UUID.randomUUID().toString(), title, clean(request.description()), clean(request.meaning()),
                DirectionStatus.from(request.status()), request.targetDate(), clean(request.coverImagePath()),
                clean(request.note()), now, now);
        dreams.save(value);
        events.record(LifeEventType.DREAM_CREATED, "dream", title, copy.dreamCreatedDescription(title),
                List.of("dream"), Map.of("dreamId", value.id(), "sourceId", value.id()));
        return value;
    }

    public synchronized Dream update(String id, DreamRequest request) {
        Dream old = get(id);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("梦想标题不能为空");
        Dream value = new Dream(id, title,
                request.description() == null ? old.description() : clean(request.description()),
                request.meaning() == null ? old.meaning() : clean(request.meaning()),
                request.status() == null ? old.status() : DirectionStatus.from(request.status()),
                request.targetDate() == null ? old.targetDate() : request.targetDate(),
                request.coverImagePath() == null ? old.coverImagePath() : clean(request.coverImagePath()),
                request.note() == null ? old.note() : clean(request.note()),
                old.createdAt(), Instant.now());
        dreams.save(value);
        return value;
    }

    /** Archiving is the soft deletion; dreams with goals/tasks are never physically removed here. */
    public synchronized Dream archive(String id) {
        Dream old = get(id);
        Dream value = withStatus(old, DirectionStatus.ARCHIVED);
        dreams.save(value);
        return value;
    }

    public synchronized Dream pause(String id) { Dream old = get(id); return save(withStatus(old, DirectionStatus.PAUSED)); }
    public synchronized Dream resume(String id) { Dream old = get(id); return save(withStatus(old, DirectionStatus.ACTIVE)); }

    /** Idempotent: completing an already-completed dream returns it without a second LifeEvent. */
    public synchronized Dream complete(String id) {
        Dream old = get(id);
        if (old.status() == DirectionStatus.COMPLETED) return old;
        Dream value = save(withStatus(old, DirectionStatus.COMPLETED));
        events.record(LifeEventType.DREAM_COMPLETED, "dream", value.title(), copy.dreamCompletedDescription(value.title()),
                List.of("dream"), Map.of("dreamId", id, "sourceId", id));
        return value;
    }

    public synchronized Goal createGoal(String dreamId, GoalRequest request) {
        get(dreamId);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("方向标题不能为空");
        Instant now = Instant.now();
        Goal value = new Goal(UUID.randomUUID().toString(), dreamId, title, clean(request.description()),
                DirectionStatus.from(request.status()), request.targetDate(),
                request.sortOrder() == null ? nextSortOrder(dreamId) : request.sortOrder(), now, now);
        goals.save(value);
        events.record(LifeEventType.GOAL_CREATED, "dream", title, copy.goalCreatedDescription(title),
                List.of("dream", "goal"), Map.of("goalId", value.id(), "dreamId", dreamId, "sourceId", value.id()));
        return value;
    }

    public synchronized Goal updateGoal(String id, GoalRequest request) {
        Goal old = goal(id);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("方向标题不能为空");
        Goal value = new Goal(id, old.dreamId(), title,
                request.description() == null ? old.description() : clean(request.description()),
                request.status() == null ? old.status() : DirectionStatus.from(request.status()),
                request.targetDate() == null ? old.targetDate() : request.targetDate(),
                request.sortOrder() == null ? old.sortOrder() : request.sortOrder(),
                old.createdAt(), Instant.now());
        goals.save(value);
        return value;
    }

    public synchronized Goal completeGoal(String id) {
        Goal old = goal(id);
        if (old.status() == DirectionStatus.COMPLETED) return old;
        Goal value = new Goal(id, old.dreamId(), old.title(), old.description(), DirectionStatus.COMPLETED,
                old.targetDate(), old.sortOrder(), old.createdAt(), Instant.now());
        goals.save(value);
        events.record(LifeEventType.GOAL_COMPLETED, "dream", value.title(), copy.goalCompletedDescription(value.title()),
                List.of("dream", "goal"), Map.of("goalId", id, "dreamId", old.dreamId(), "sourceId", id));
        return value;
    }

    /** Deleting a Goal clears the direction links of any tasks referencing it or its milestones. */
    public synchronized void deleteGoal(String id) {
        goal(id);
        List<String> milestoneIds = milestonesOf(id).stream().map(DreamMilestone::id).toList();
        goals.delete(id);
        clearTaskReferences(Set.of(), Set.of(id), new HashSet<>(milestoneIds));
    }

    public synchronized DreamMilestone createMilestone(String goalId, DreamMilestoneRequest request) {
        goal(goalId);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("里程碑标题不能为空");
        Instant now = Instant.now();
        DreamMilestone value = new DreamMilestone(UUID.randomUUID().toString(), goalId, title,
                clean(request.description()), DreamMilestoneStatus.from(request.status()), request.targetDate(),
                null, request.sortOrder() == null ? nextMilestoneSortOrder(goalId) : request.sortOrder(), now, now);
        milestones.save(value);
        return value;
    }

    public synchronized DreamMilestone updateMilestone(String id, DreamMilestoneRequest request) {
        DreamMilestone old = milestone(id);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("里程碑标题不能为空");
        DreamMilestone value = new DreamMilestone(id, old.goalId(), title,
                request.description() == null ? old.description() : clean(request.description()),
                request.status() == null ? old.status() : DreamMilestoneStatus.from(request.status()),
                request.targetDate() == null ? old.targetDate() : request.targetDate(),
                old.completedAt(),
                request.sortOrder() == null ? old.sortOrder() : request.sortOrder(),
                old.createdAt(), Instant.now());
        milestones.save(value);
        return value;
    }

    /** Idempotent completion records completedAt once and emits the fact exactly once. */
    public synchronized DreamMilestone completeMilestone(String id) {
        DreamMilestone old = milestone(id);
        if (old.status() == DreamMilestoneStatus.COMPLETED) return old;
        Instant now = Instant.now();
        DreamMilestone value = new DreamMilestone(id, old.goalId(), old.title(), old.description(),
                DreamMilestoneStatus.COMPLETED, old.targetDate(), now, old.sortOrder(), old.createdAt(), now);
        milestones.save(value);
        events.record(LifeEventType.DREAM_MILESTONE_COMPLETED, "dream", value.title(),
                copy.dreamMilestoneCompletedDescription(value.title()), List.of("dream", "milestone"),
                Map.of("dreamMilestoneId", id, "goalId", old.goalId(), "sourceId", id));
        return value;
    }

    public synchronized void deleteMilestone(String id) {
        milestone(id);
        milestones.delete(id);
        clearTaskReferences(Set.of(), Set.of(), Set.of(id));
    }

    /** Resolves display titles for a task's direction links; dangling ids resolve to empty strings. */
    public DirectionInfo resolve(String dreamId, String goalId, String milestoneId) {
        String dreamTitle = dreamId == null || dreamId.isBlank() ? null
                : dreams.find(dreamId).map(Dream::title).orElse("");
        String goalTitle = goalId == null || goalId.isBlank() ? null
                : goals.find(goalId).map(Goal::title).orElse("");
        String milestoneTitle = milestoneId == null || milestoneId.isBlank() ? null
                : milestones.find(milestoneId).map(DreamMilestone::title).orElse("");
        return new DirectionInfo(dreamId, dreamTitle, goalId, goalTitle, milestoneId, milestoneTitle);
    }

    /** Infers the full chain from the deepest provided id and rejects cross-level conflicts. */
    public DirectionInfo resolveLink(String dreamId, String goalId, String milestoneId) {
        if (milestoneId != null && !milestoneId.isBlank()) {
            DreamMilestone m = milestone(milestoneId);
            Goal g = goal(m.goalId());
            if (goalId != null && !goalId.isBlank() && !goalId.equals(g.id()))
                throw bad("里程碑与指定的方向不一致");
            if (dreamId != null && !dreamId.isBlank() && !dreamId.equals(g.dreamId()))
                throw bad("里程碑与指定的梦想不一致");
            return new DirectionInfo(g.dreamId(), dreamTitle(g.dreamId()), g.id(), g.title(), m.id(), m.title());
        }
        if (goalId != null && !goalId.isBlank()) {
            Goal g = goal(goalId);
            if (dreamId != null && !dreamId.isBlank() && !dreamId.equals(g.dreamId()))
                throw bad("方向与指定的梦想不一致");
            return new DirectionInfo(g.dreamId(), dreamTitle(g.dreamId()), g.id(), g.title(), "", null);
        }
        if (dreamId != null && !dreamId.isBlank() && dreams.find(dreamId).isEmpty()) throw bad("梦想不存在");
        return resolve(dreamId, goalId, milestoneId);
    }

    public void clearTaskReferences(String dreamId, String goalId, String milestoneId) {
        clearTaskReferences(dreamId == null ? Set.of() : Set.of(dreamId),
                goalId == null ? Set.of() : Set.of(goalId),
                milestoneId == null ? Set.of() : Set.of(milestoneId));
    }

    public void clearTaskReferences(Set<String> dreamIds, Set<String> goalIds, Set<String> milestoneIds) {
        links.ifAvailable(service -> service.clearReferences(dreamIds, goalIds, milestoneIds));
    }

    private Dream save(Dream value) { dreams.save(value); return value; }
    private Dream withStatus(Dream old, DirectionStatus status) {
        return new Dream(old.id(), old.title(), old.description(), old.meaning(), status, old.targetDate(),
                old.coverImagePath(), old.note(), old.createdAt(), Instant.now());
    }
    private Goal goal(String id) { return goals.find(id).orElseThrow(this::missingGoal); }
    private DreamMilestone milestone(String id) { return milestones.find(id).orElseThrow(this::missingMilestone); }
    private String dreamTitle(String id) { return dreams.find(id).map(Dream::title).orElse(""); }
    private int nextSortOrder(String dreamId) { return goals.byDream(dreamId).size(); }
    private int nextMilestoneSortOrder(String goalId) { return milestones.byGoal(goalId).size(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missingDream() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "梦想不存在"); }
    private ResponseStatusException missingGoal() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "方向不存在"); }
    private ResponseStatusException missingMilestone() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "梦想里程碑不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
