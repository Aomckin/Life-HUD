package io.github.aomckin.lifehud.domain;

import java.time.Instant;

public record EnergyRecord(String id, int delta, int before, int after, String reason,
                           String sourceEventId, Instant occurredAt) { }
