package io.github.aomckin.lifehud.domain;

import java.time.LocalDate;
import java.util.List;

public record MilestoneRequest(String title, String description, LocalDate occurredAt,
                               String category, List<String> relatedEventIds,
                               String relatedModule, String media, Boolean pinned) { }
