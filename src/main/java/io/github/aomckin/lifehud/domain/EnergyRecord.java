package io.github.aomckin.lifehud.domain;

import java.time.Instant;

public record EnergyRecord(String id, int delta, int before, int after, String reason,
                           String sourceEventId, Instant occurredAt, EnergyChangeType type, int requestedDelta) {
    public EnergyRecord {
        if (type == null) type = delta < 0 ? EnergyChangeType.SPEND : EnergyChangeType.EARN;
        if (requestedDelta == 0) requestedDelta = delta;
    }
}
