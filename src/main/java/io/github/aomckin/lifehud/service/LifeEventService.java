package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.LifeEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Single write/read entry point for life history; feature modules never access its repository directly. */
@Service
public final class LifeEventService {
    private final LifeEventRepository events;
    private final ObjectProvider<GrowthEngine> growth;
    public LifeEventService(LifeEventRepository events) { this(events, null); }
    @Autowired public LifeEventService(LifeEventRepository events, ObjectProvider<GrowthEngine> growth) {
        this.events = events; this.growth = growth;
    }
    public LifeEvent record(LifeEventType type, String source, String title, String content, List<String> tags, Map<String, Object> metadata) {
        return recordAt(type, source, title, content, Instant.now(), tags, metadata);
    }

    /** Backfillable variant: the fact is dated when it happened, not when it was recorded. */
    public LifeEvent recordAt(LifeEventType type, String source, String title, String content, Instant occurredAt,
                              List<String> tags, Map<String, Object> metadata) {
        Instant now = Instant.now();
        LifeEvent event = new LifeEvent(UUID.randomUUID().toString(), type, source, title, content, occurredAt, now, List.copyOf(tags), Map.copyOf(metadata));
        events.append(event);
        if (growth != null) growth.ifAvailable(engine -> engine.process(event));
        return event;
    }
    public LifeEvent recordDerived(LifeEventType type, String source, String title, String content,
                                   List<String> tags, Map<String, Object> metadata) {
        Instant now = Instant.now();
        LifeEvent event = new LifeEvent(UUID.randomUUID().toString(), type, source, title, content,
                now, now, List.copyOf(tags), Map.copyOf(metadata));
        events.append(event);
        if (growth != null) growth.ifAvailable(GrowthEngine::recordDerivedEvent);
        return event;
    }
    public List<LifeEvent> recent(int limit) { return events.recent(limit); }
    public List<LifeEvent> all() { return events.all(); }

    public Optional<LifeEvent> find(String id) {
        return events.all().stream().filter(event -> event.id().equals(id)).findFirst();
    }

    /** The one fact emitted for a business record; sourceId is the business record id. */
    public Optional<LifeEvent> findBySource(LifeEventType type, String sourceId) {
        return events.all().stream()
                .filter(event -> event.type() == type && event.sourceId().equals(sourceId))
                .findFirst();
    }

    /** Editing a business record rewrites its fact in place; growth already settled on creation. */
    public LifeEvent replace(LifeEvent event) { events.replace(event); return event; }

    public boolean delete(String eventId) { return events.delete(eventId); }
}
