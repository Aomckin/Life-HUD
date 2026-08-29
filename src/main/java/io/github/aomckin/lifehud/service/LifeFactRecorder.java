package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.LifeEventSourceType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Uniform record → fact sync for the v0.6 life modules. One business record owns
 * exactly one RECORDED LifeEvent, found via metadata sourceId:
 *   create → emit the fact once (idempotent: a retry reuses the same sourceId check)
 *   edit   → rewrite that fact in place (version+1, occurredAt re-dated)
 *   delete → remove the fact so the timeline keeps no ghosts
 */
@Component
public final class LifeFactRecorder {
    private final LifeEventService events;

    public LifeFactRecorder(LifeEventService events) { this.events = events; }

    public void recordCreated(LifeEventType type, String source, String sourceId, String title, String content,
                              Instant occurredAt, List<String> tags, Map<String, Object> metadata) {
        if (events.findBySource(type, sourceId).isPresent()) return; // retry of the same save
        events.recordAt(type, source, title, content, occurredAt, tags, withSource(metadata, sourceId));
    }

    public void recordUpdated(LifeEventType type, String source, String sourceId, String title, String content,
                              Instant occurredAt, List<String> tags, Map<String, Object> metadata) {
        LifeEvent existing = events.findBySource(type, sourceId).orElse(null);
        if (existing == null) { recordCreated(type, source, sourceId, title, content, occurredAt, tags, metadata); return; }
        LifeEvent value = new LifeEvent(existing.id(), type, source, title, content, occurredAt, existing.createdAt(),
                tags, withSource(metadata, sourceId), LifeEventSourceType.from(source), sourceId,
                content, existing.version() + 1);
        events.replace(value);
    }

    public void recordDeleted(LifeEventType type, String sourceId) {
        events.findBySource(type, sourceId).ifPresent(event -> events.delete(event.id()));
    }

    private Map<String, Object> withSource(Map<String, Object> metadata, String sourceId) {
        java.util.Map<String, Object> merged = new java.util.HashMap<>(metadata == null ? Map.of() : metadata);
        merged.put("sourceId", sourceId);
        return Map.copyOf(merged);
    }
}
