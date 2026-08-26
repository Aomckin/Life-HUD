package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.LifeEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Single write/read entry point for life history; feature modules never access its repository directly. */
@Service
public final class LifeEventService {
    private final LifeEventRepository events;
    public LifeEventService(LifeEventRepository events) { this.events = events; }
    public LifeEvent record(LifeEventType type, String source, String title, String content, List<String> tags, Map<String, Object> metadata) {
        Instant now = Instant.now();
        LifeEvent event = new LifeEvent(UUID.randomUUID().toString(), type, source, title, content, now, now, List.copyOf(tags), Map.copyOf(metadata));
        events.append(event);
        return event;
    }
    public List<LifeEvent> recent(int limit) { return events.recent(limit); }
}
