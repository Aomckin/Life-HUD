package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.time.*;
import org.springframework.stereotype.Service;

/** Maintains O(1) growth counters. Historical scans are restricted to explicit migration/rebuild. */
@Service
public final class GrowthStatsService {
    private final GrowthStatsRepository stats;
    private final FocusSessionRepository focus;
    private final LifeEventRepository events;
    private final MilestoneRepository milestones;

    public GrowthStatsService(GrowthStatsRepository stats, FocusSessionRepository focus,
                              LifeEventRepository events, MilestoneRepository milestones) {
        this.stats = stats;
        this.focus = focus;
        this.events = events;
        this.milestones = milestones;
    }

    public synchronized GrowthStats current() {
        if (!stats.exists()) return rebuild();
        return stats.load();
    }

    public synchronized GrowthStats apply(LifeEvent event) {
        if (!stats.exists()) return rebuild();
        GrowthStats old = current();
        long iron = old.ironFocusCount(), pomodoro = old.pomodoroFocusCount(), focusCount = old.focusCount();
        long actual = old.actualFocusMinutes(), effective = old.effectiveFocusMinutes();
        long maximum = old.maxEffectiveFocusMinutes(), cross = old.crossMidnightFocusCount();
        long tasks = old.taskCount(), eventCount = old.lifeEventCount() + 1, milestoneCount = old.milestoneCount();
        if (event.type() == LifeEventType.FOCUS_FINISHED) {
            long actualMinutes = number(event, "actualSeconds") / 60;
            long effectiveMinutes = number(event, "effectiveSeconds") / 60;
            focusCount++;
            actual += actualMinutes;
            effective += effectiveMinutes;
            maximum = Math.max(maximum, effectiveMinutes);
            String mode = String.valueOf(event.metadata().getOrDefault("mode", ""));
            if ("IRON_CURTAIN".equalsIgnoreCase(mode)) iron++;
            if ("POMODORO".equalsIgnoreCase(mode)) pomodoro++;
            if (Boolean.TRUE.equals(event.metadata().get("crossMidnight"))) cross++;
        } else if (event.type() == LifeEventType.TASK_COMPLETED) tasks++;
        else if (event.type() == LifeEventType.MILESTONE_CREATED) milestoneCount++;
        GrowthStats next = new GrowthStats(iron, pomodoro, focusCount, actual, effective, maximum,
                cross, tasks, eventCount, milestoneCount, Instant.now(), 1);
        stats.save(next);
        return next;
    }

    public synchronized void recordDerivedEvent() {
        GrowthStats old = current();
        stats.save(new GrowthStats(old.ironFocusCount(), old.pomodoroFocusCount(), old.focusCount(),
                old.actualFocusMinutes(), old.effectiveFocusMinutes(), old.maxEffectiveFocusMinutes(),
                old.crossMidnightFocusCount(), old.taskCount(), old.lifeEventCount() + 1,
                old.milestoneCount(), Instant.now(), old.schemaVersion()));
    }

    public synchronized GrowthStats rebuild() {
        Instant now = Instant.now();
        var sessions = focus.all().stream().filter(s -> s.status() == FocusStatus.COMPLETED || s.status() == FocusStatus.INTERRUPTED).toList();
        ZoneId zone = ZoneId.systemDefault();
        long iron = sessions.stream().filter(s -> s.mode() == FocusMode.IRON_CURTAIN).count();
        long pomodoro = sessions.stream().filter(s -> s.mode() == FocusMode.POMODORO).count();
        long actual = sessions.stream().mapToLong(s -> s.actualSeconds(s.endedAt() == null ? now : s.endedAt()) / 60).sum();
        long effective = sessions.stream().mapToLong(s -> s.effectiveSeconds(s.endedAt() == null ? now : s.endedAt()) / 60).sum();
        long maximum = sessions.stream().mapToLong(s -> s.effectiveSeconds(s.endedAt() == null ? now : s.endedAt()) / 60).max().orElse(0);
        long cross = sessions.stream().filter(s -> s.endedAt() != null &&
                !s.startedAt().atZone(zone).toLocalDate().equals(s.endedAt().atZone(zone).toLocalDate())).count();
        long tasks = events.all().stream().filter(e -> e.type() == LifeEventType.TASK_COMPLETED).count();
        GrowthStats rebuilt = new GrowthStats(iron, pomodoro, sessions.size(), actual, effective, maximum,
                cross, tasks, events.all().size(), milestones.all().size(), now, 1);
        stats.save(rebuilt);
        return rebuilt;
    }

    private long number(LifeEvent event, String key) {
        Object value = event.metadata().get(key);
        return value instanceof Number number ? Math.max(0, number.longValue()) : 0;
    }
}
