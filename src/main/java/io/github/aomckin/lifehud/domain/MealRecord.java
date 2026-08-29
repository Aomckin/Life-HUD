package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** One meal fact: when, what, how it felt, plus photos. No nutrition math in v0.6. */
public record MealRecord(String id, MealType mealType, Instant time, String description,
                         Integer satisfaction, String note, List<String> images,
                         Instant createdAt, Instant updatedAt) {
    public MealRecord {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
