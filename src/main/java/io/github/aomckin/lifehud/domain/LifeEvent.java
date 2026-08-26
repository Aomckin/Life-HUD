package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A durable, display-ready record of something that happened in the user's life HUD. */
public record LifeEvent(String id, LifeEventType type, String source, String title, String content,
                        Instant occurredAt, Instant createdAt, List<String> tags, Map<String, Object> metadata) { }
