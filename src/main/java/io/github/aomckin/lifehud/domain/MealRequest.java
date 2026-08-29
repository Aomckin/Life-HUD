package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

public record MealRequest(String mealType, Instant time, String description, Integer satisfaction,
                          String note, List<String> images) { }
