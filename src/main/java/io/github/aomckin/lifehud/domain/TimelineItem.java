package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Display-ready timeline entry assembled from a LifeEvent. Never expose the raw
 * entity: metadata here carries only what the UI actually renders.
 */
public record TimelineItem(String eventId, String type, String source, String sourceId, Instant occurredAt,
                           String title, String summary, List<String> tags, List<String> media,
                           Map<String, Object> metadata) {
    public TimelineItem {
        tags = tags == null ? List.of() : List.copyOf(tags);
        media = media == null ? List.of() : List.copyOf(media);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
