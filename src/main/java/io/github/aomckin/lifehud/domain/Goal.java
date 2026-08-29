package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalDate;

/** An executable direction under a Dream; must always belong to one. */
public record Goal(String id, String dreamId, String title, String description, DirectionStatus status,
                   LocalDate targetDate, int sortOrder, Instant createdAt, Instant updatedAt) { }
