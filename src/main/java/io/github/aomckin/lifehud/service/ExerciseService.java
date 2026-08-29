package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.ExerciseRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exercise facts: what, when it started, how long, how hard. No heart rate,
 * GPS or calorie estimation in v0.6.
 */
@Service
public final class ExerciseService {
    private final ExerciseRecordRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public ExerciseService(ExerciseRecordRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<ExerciseRecord> all() { return records.all(); }
    public ExerciseRecord get(String id) { return records.find(id).orElseThrow(this::missing); }

    public synchronized ExerciseRecord create(ExerciseRequest request) {
        ExerciseType type = type(request == null ? null : request.type());
        Integer duration = request == null ? null : request.durationMinutes();
        if (duration == null || duration < 1 || duration > 24 * 60) throw bad("运动时长需要在 1 ~ 1440 分钟之间");
        Instant startTime = request == null || request.startTime() == null ? Instant.now() : request.startTime();
        ExerciseIntensity intensity = intensity(request == null ? null : request.intensity());
        Instant now = Instant.now();
        ExerciseRecord value = new ExerciseRecord(UUID.randomUUID().toString(), type, startTime, duration,
                intensity, clean(request == null ? null : request.note()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.EXERCISE_RECORDED, "exercise", value.id(),
                copy.lifeExerciseType(type.name()), summary(value), startTime,
                List.of("life", "exercise"), metadata(value));
        return value;
    }

    public synchronized ExerciseRecord update(String id, ExerciseRequest request) {
        ExerciseRecord old = get(id);
        Integer duration = request.durationMinutes() == null ? old.durationMinutes() : request.durationMinutes();
        if (duration < 1 || duration > 24 * 60) throw bad("运动时长需要在 1 ~ 1440 分钟之间");
        ExerciseRecord value = new ExerciseRecord(id,
                request.type() == null ? old.type() : type(request.type()),
                request.startTime() == null ? old.startTime() : request.startTime(),
                duration,
                request.intensity() == null ? old.intensity() : intensity(request.intensity()),
                request.note() == null ? old.note() : clean(request.note()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.EXERCISE_RECORDED, "exercise", id,
                copy.lifeExerciseType(value.type().name()), summary(value), value.startTime(),
                List.of("life", "exercise"), metadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.EXERCISE_RECORDED, id);
    }

    private String summary(ExerciseRecord value) {
        return value.durationMinutes() + " min · " + copy.lifeExerciseIntensity(value.intensity().name());
    }
    private Map<String, Object> metadata(ExerciseRecord value) {
        return Map.of("durationMinutes", value.durationMinutes(), "intensity", value.intensity().name());
    }
    private ExerciseType type(String type) {
        try { return ExerciseType.valueOf(type == null ? "OTHER" : type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ExerciseType.OTHER; }
    }
    private ExerciseIntensity intensity(String intensity) {
        try { return ExerciseIntensity.valueOf(intensity == null ? "MEDIUM" : intensity.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ExerciseIntensity.MEDIUM; }
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "运动记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
