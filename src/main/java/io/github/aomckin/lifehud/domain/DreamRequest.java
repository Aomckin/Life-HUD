package io.github.aomckin.lifehud.domain;

import java.time.LocalDate;

/** Request body for creating or updating a Dream. */
public record DreamRequest(String title, String description, String meaning, String status,
                           LocalDate targetDate, String coverImagePath, String note) { }
