package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalDate;

/** A dream the user is chasing; facts only, Growth consumes its events. */
public record Dream(String id, String title, String description, String meaning, DirectionStatus status,
                    LocalDate targetDate, String coverImagePath, String note, Instant createdAt, Instant updatedAt) { }
