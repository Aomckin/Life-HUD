package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalDate;

/** A staged node under a Goal. Named DreamMilestone to stay distinct from the Growth Milestone. */
public record DreamMilestone(String id, String goalId, String title, String description,
                             DreamMilestoneStatus status, LocalDate targetDate, Instant completedAt,
                             int sortOrder, Instant createdAt, Instant updatedAt) { }
