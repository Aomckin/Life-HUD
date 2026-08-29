package io.github.aomckin.lifehud.domain;

import java.time.LocalDate;

/** Request body for creating or updating a Goal under a Dream. */
public record GoalRequest(String title, String description, String status, LocalDate targetDate, Integer sortOrder) { }
