package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.Map;

/** CUSTOM records carry their label in metadata("label"). */
public record LifeRecordRequest(String type, Double value, String unit, Instant time,
                                String note, Map<String, Object> metadata) { }
