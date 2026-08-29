package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.SleepRecordRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sleep facts. The record is the truth; SLEEP_RECORDED is its timeline voice.
 * durationMinutes is derived server-side so cross-midnight and backfilled
 * sleeps can never disagree with their timestamps.
 */
@Service
public final class SleepService {
    /** Anything beyond this is a data-entry mistake, not a nap. */
    private static final int MAX_DURATION_MINUTES = 20 * 60;
    private final SleepRecordRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public SleepService(SleepRecordRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<SleepRecord> all() { return records.all(); }
    public SleepRecord get(String id) { return records.find(id).orElseThrow(this::missing); }

    public synchronized SleepRecord create(SleepRequest request) {
        Instant sleepTime = request == null ? null : request.sleepTime();
        Instant wakeTime = request == null ? null : request.wakeTime();
        if (sleepTime == null || wakeTime == null) throw bad("需要入睡与起床时间");
        int duration = durationOf(sleepTime, wakeTime);
        Instant now = Instant.now();
        SleepRecord value = new SleepRecord(java.util.UUID.randomUUID().toString(), sleepTime, wakeTime, duration,
                quality(request.quality()), type(request.type()), clean(request.note()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.SLEEP_RECORDED, "sleep", value.id(), copy.lifeSleepTitle(),
                summary(value), wakeTime, List.of("life", "sleep"), metadata(value));
        return value;
    }

    public synchronized SleepRecord update(String id, SleepRequest request) {
        SleepRecord old = get(id);
        Instant sleepTime = request.sleepTime() == null ? old.sleepTime() : request.sleepTime();
        Instant wakeTime = request.wakeTime() == null ? old.wakeTime() : request.wakeTime();
        SleepRecord value = new SleepRecord(id, sleepTime, wakeTime, durationOf(sleepTime, wakeTime),
                request.quality() == null ? old.quality() : quality(request.quality()),
                request.type() == null ? old.type() : type(request.type()),
                request.note() == null ? old.note() : clean(request.note()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.SLEEP_RECORDED, "sleep", id, copy.lifeSleepTitle(),
                summary(value), value.wakeTime(), List.of("life", "sleep"), metadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.SLEEP_RECORDED, id);
    }

    private int durationOf(Instant sleepTime, Instant wakeTime) {
        if (!wakeTime.isAfter(sleepTime)) throw bad("起床时间必须晚于入睡时间");
        long minutes = Duration.between(sleepTime, wakeTime).toMinutes();
        if (minutes > MAX_DURATION_MINUTES) throw bad("睡眠时长异常：超过 20 小时");
        return (int) minutes;
    }

    private String summary(SleepRecord value) {
        long hours = value.durationMinutes() / 60;
        long minutes = value.durationMinutes() % 60;
        String duration = hours > 0 ? hours + "h " + minutes + "min" : minutes + "min";
        return copy.lifeSleepSummary(duration, value.quality());
    }

    private Map<String, Object> metadata(SleepRecord value) {
        return Map.of("durationMinutes", value.durationMinutes(), "quality", value.quality(),
                "sleepType", value.type().name());
    }

    private int quality(Integer quality) {
        if (quality == null) return 3;
        return Math.max(1, Math.min(5, quality));
    }

    private SleepType type(String type) {
        try { return SleepType.valueOf(type == null ? "NIGHT" : type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return SleepType.NIGHT; }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "睡眠记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
