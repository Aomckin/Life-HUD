package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.time.LocalTime;

/** A repeatable state-entry ritual ("进入一种状态"), distinct from a one-shot Task. */
public record Ritual(String id, String name, String description, String category,
                     LocalTime triggerTime, boolean enabled, Instant createdAt, Instant updatedAt) { }
