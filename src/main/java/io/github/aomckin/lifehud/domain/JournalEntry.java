package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** A manual journal entry; plain long text with optional tags and photos. */
public record JournalEntry(String id, String content, Instant occurredAt, List<String> images,
                           List<String> tags, Instant createdAt, Instant updatedAt) {
    public JournalEntry {
        images = images == null ? List.of() : List.copyOf(images);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
