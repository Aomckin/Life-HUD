package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.MealRecordRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Meal facts: when, what, how it felt, photos. v0.6 records exactly that —
 * no calories, no nutrition analysis.
 */
@Service
public final class MealService {
    private final MealRecordRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public MealService(MealRecordRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<MealRecord> all() { return records.all(); }
    public MealRecord get(String id) { return records.find(id).orElseThrow(this::missing); }

    public synchronized MealRecord create(MealRequest request) {
        MealType mealType = type(request == null ? null : request.mealType());
        Instant time = request == null || request.time() == null ? Instant.now() : request.time();
        Integer satisfaction = request == null ? null : satisfaction(request.satisfaction());
        Instant now = Instant.now();
        MealRecord value = new MealRecord(UUID.randomUUID().toString(), mealType, time,
                clean(request == null ? null : request.description()), satisfaction,
                clean(request == null ? null : request.note()),
                images(request == null ? null : request.images()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.MEAL_RECORDED, "meal", value.id(), copy.lifeMealType(mealType.name()),
                summary(value), time, List.of("life", "meal"), metadata(value));
        return value;
    }

    public synchronized MealRecord update(String id, MealRequest request) {
        MealRecord old = get(id);
        MealRecord value = new MealRecord(id,
                request.mealType() == null ? old.mealType() : type(request.mealType()),
                request.time() == null ? old.time() : request.time(),
                request.description() == null ? old.description() : clean(request.description()),
                request.satisfaction() == null ? old.satisfaction() : satisfaction(request.satisfaction()),
                request.note() == null ? old.note() : clean(request.note()),
                request.images() == null ? old.images() : images(request.images()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.MEAL_RECORDED, "meal", id, copy.lifeMealType(value.mealType().name()),
                summary(value), value.time(), List.of("life", "meal"), metadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.MEAL_RECORDED, id);
    }

    /** Paths point into the shared upload storage; blank entries are dropped. */
    private List<String> images(List<String> images) {
        if (images == null) return List.of();
        return images.stream().filter(path -> path != null && !path.isBlank()).map(String::trim).toList();
    }

    private String summary(MealRecord value) {
        String satisfaction = copy.lifeMealSummary(value.satisfaction());
        if (value.description() == null || value.description().isBlank()) return satisfaction;
        return satisfaction.isEmpty() ? value.description() : value.description() + " · " + satisfaction;
    }
    private Map<String, Object> metadata(MealRecord value) {
        Map<String, Object> metadata = new HashMap<>();
        if (!value.images().isEmpty()) metadata.put("images", value.images());
        if (value.satisfaction() != null) metadata.put("satisfaction", value.satisfaction());
        return metadata;
    }
    private Integer satisfaction(Integer satisfaction) {
        return satisfaction == null ? null : Math.max(1, Math.min(5, satisfaction));
    }
    private MealType type(String type) {
        try { return MealType.valueOf(type == null ? "OTHER" : type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return MealType.OTHER; }
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "饮食记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
