package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** Request body for creating or updating an entertainment record. */
public record EntertainmentRequest(String category, String title, Integer durationMinutes, Integer energyCost,
                                   String note, Instant occurredAt) { }
