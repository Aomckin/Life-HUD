package io.github.aomckin.lifehud.domain;

import java.time.LocalDate;

/** Request body for creating or updating a DreamMilestone under a Goal. */
public record DreamMilestoneRequest(String title, String description, String status,
                                    LocalDate targetDate, Integer sortOrder) { }
