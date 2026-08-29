package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/**
 * A lightweight record of a real entertainment moment. It only records what happened;
 * how much Energy was actually consumed is settled by the Growth ledger, and how much
 * EXP that consumption yields is settled by GrowthEngine.
 */
public record EntertainmentRecord(String id, EntertainmentCategory category, String title, int durationMinutes,
                                  int energyCost, String note, Instant occurredAt, String lifeEventId,
                                  Instant createdAt, Instant updatedAt) { }
