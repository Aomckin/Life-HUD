package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventSourceType;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.TimelineItem;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * The unified life timeline. Aggregation is a backend duty: the frontend asks
 * one question ("what happened in this range / group?") instead of stitching
 * ten business feeds together. Items are ordered by occurredAt, newest first.
 */
@Service
public final class TimelineService {
    /** Coarse filter groups shown as tabs; each maps to one or more sources. */
    private static final Map<String, Set<LifeEventSourceType>> GROUPS = Map.of(
            "focus", EnumSet.of(LifeEventSourceType.FOCUS),
            "task", EnumSet.of(LifeEventSourceType.TASK),
            "life", EnumSet.of(LifeEventSourceType.SLEEP, LifeEventSourceType.MEAL,
                    LifeEventSourceType.EXERCISE, LifeEventSourceType.CHECK_IN, LifeEventSourceType.LIFE_RECORD),
            "journal", EnumSet.of(LifeEventSourceType.JOURNAL),
            "growth", EnumSet.of(LifeEventSourceType.LEVEL, LifeEventSourceType.ACHIEVEMENT,
                    LifeEventSourceType.TITLE, LifeEventSourceType.MILESTONE),
            "ritual", EnumSet.of(LifeEventSourceType.RITUAL),
            "now", EnumSet.of(LifeEventSourceType.NOW),
            "media", EnumSet.of(LifeEventSourceType.MEDIA),
            "dream", EnumSet.of(LifeEventSourceType.DREAM));

    private final LifeEventService events;

    public TimelineService(LifeEventService events) { this.events = events; }

    public List<TimelineItem> timeline(String date, String startDate, String endDate, String sources,
                                       String type, int page, int size) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate from = startDate == null || startDate.isBlank() ? null : parseDate(startDate);
        LocalDate to = endDate == null || endDate.isBlank() ? null : parseDate(endDate);
        if (date != null && !date.isBlank()) { from = parseDate(date); to = from; }
        if (from != null && to != null && to.isBefore(from)) throw bad("结束日期不能早于开始日期");
        Set<LifeEventSourceType> wanted = wantedSources(sources);
        LifeEventType eventType = parseType(type);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 200));

        List<LifeEvent> matched = new ArrayList<>();
        for (LifeEvent event : events.all()) {
            LocalDate day = LocalDate.ofInstant(event.occurredAt(), zone);
            if (from != null && day.isBefore(from)) continue;
            if (to != null && day.isAfter(to)) continue;
            if (!wanted.isEmpty() && !wanted.contains(event.sourceType())) continue;
            if (eventType != null && event.type() != eventType) continue;
            matched.add(event);
        }
        matched.sort(Comparator.comparing(LifeEvent::occurredAt).reversed());
        return matched.stream()
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .map(TimelineService::toItem)
                .toList();
    }

    private static TimelineItem toItem(LifeEvent event) {
        Map<String, Object> metadata = new java.util.HashMap<>(event.metadata());
        metadata.remove("sourceId");
        return new TimelineItem(event.id(), event.type().name(), event.source(), event.sourceId(),
                event.occurredAt(), event.title(), event.description(), event.tags(),
                media(event), metadata);
    }

    @SuppressWarnings("unchecked")
    private static List<String> media(LifeEvent event) {
        Object images = event.metadata().get("images");
        return images instanceof List<?> list ? (List<String>) list : List.of();
    }

    private Set<LifeEventSourceType> wantedSources(String sources) {
        if (sources == null || sources.isBlank()) return Set.of();
        Set<LifeEventSourceType> wanted = new LinkedHashSet<>();
        for (String token : sources.split(",")) {
            String name = token.trim();
            if (name.isEmpty()) continue;
            String group = name.toLowerCase();
            if (GROUPS.containsKey(group)) { wanted.addAll(GROUPS.get(group)); continue; }
            try { wanted.add(LifeEventSourceType.valueOf(name.toUpperCase())); }
            catch (IllegalArgumentException ignored) { throw bad("未知的事件来源：" + name); }
        }
        return wanted;
    }

    private LifeEventType parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try { return LifeEventType.valueOf(type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { throw bad("未知的事件类型：" + type); }
    }

    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value.trim()); }
        catch (Exception ignored) { throw bad("日期格式应为 yyyy-MM-dd：" + value); }
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
