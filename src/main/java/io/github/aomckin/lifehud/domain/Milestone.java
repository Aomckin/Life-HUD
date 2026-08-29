package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Milestone(String id, String title, String description, LocalDate occurredAt,
                        String category, MilestoneSource source, List<String> relatedEventIds,
                        String relatedModule, String media, boolean pinned,
                        Instant createdAt, Instant updatedAt) {
    public Milestone { relatedEventIds = relatedEventIds == null ? List.of() : List.copyOf(relatedEventIds); }
}
