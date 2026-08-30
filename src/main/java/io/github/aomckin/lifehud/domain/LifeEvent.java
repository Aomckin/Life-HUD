package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A durable, display-ready record of something that happened in the user's life HUD. */
public record LifeEvent(String id, LifeEventType type, String source, String title, String content,
                        Instant occurredAt, Instant createdAt, List<String> tags, Map<String, Object> metadata,
                        LifeEventSourceType sourceType, String sourceId, String description, int version) {
    public LifeEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        tags = tags == null ? List.of() : List.copyOf(tags);
        sourceType = sourceType == null ? LifeEventSourceType.from(source) : sourceType;
        // v0.5.5 wrote source="now" before NOW existed, so persisted events were
        // classified as SYSTEM. Normalize on read without rewriting user data.
        if ("now".equalsIgnoreCase(source) && sourceType == LifeEventSourceType.SYSTEM)
            sourceType = LifeEventSourceType.NOW;
        sourceId = sourceId == null ? inferredSourceId(metadata) : sourceId;
        description = description == null ? content : description;
        version = version < 1 ? 1 : version;
    }

    /** Source-compatible constructor for callers and stored v0.2-v0.3 events. */
    public LifeEvent(String id, LifeEventType type, String source, String title, String content,
                     Instant occurredAt, Instant createdAt, List<String> tags, Map<String, Object> metadata) {
        this(id, type, source, title, content, occurredAt, createdAt, tags, metadata,
                LifeEventSourceType.from(source), inferredSourceId(metadata), content, 1);
    }

    private static String inferredSourceId(Map<String, Object> metadata) {
        if (metadata == null) return "";
        return String.valueOf(metadata.getOrDefault("sourceId",
                metadata.getOrDefault("focusSessionId", metadata.getOrDefault("taskId",
                        metadata.getOrDefault("milestoneId", "")))));
    }
}
