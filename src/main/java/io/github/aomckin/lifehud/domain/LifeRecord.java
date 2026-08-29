package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.Map;

/** One generic life observation (water, caffeine, sunlight, ...). */
public record LifeRecord(String id, LifeRecordType type, double value, String unit, Instant time,
                         String note, Map<String, Object> metadata, Instant createdAt, Instant updatedAt) {
    public LifeRecord {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
