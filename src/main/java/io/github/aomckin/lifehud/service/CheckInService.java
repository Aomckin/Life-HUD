package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.CheckInRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 10-second state snapshots. All four scales are clamped to 1~10 server-side;
 * the latest entry is the "最近状态" card, never a live feed.
 */
@Service
public final class CheckInService {
    private final CheckInRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public CheckInService(CheckInRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<CheckIn> all() { return records.all(); }
    public CheckIn get(String id) { return records.find(id).orElseThrow(this::missing); }
    public CheckIn latest() {
        return records.all().stream().max(java.util.Comparator.comparing(CheckIn::time)).orElse(null);
    }

    public synchronized CheckIn create(CheckInRequest request) {
        Integer energy = request == null ? null : request.energy();
        Integer mood = request == null ? null : request.mood();
        Integer focusDesire = request == null ? null : request.focusDesire();
        Integer fatigue = request == null ? null : request.fatigue();
        if (energy == null || mood == null || focusDesire == null || fatigue == null)
            throw bad("能量、心情、专注意愿和疲劳都需要打分");
        Instant time = request.time() == null ? Instant.now() : request.time();
        Instant now = Instant.now();
        CheckIn value = new CheckIn(UUID.randomUUID().toString(), scale(energy), scale(mood), scale(focusDesire),
                scale(fatigue), time, clean(request.note()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.CHECK_IN_RECORDED, "check_in", value.id(), copy.lifeCheckInTitle(),
                summary(value), time, List.of("life", "check-in"), metadata(value));
        return value;
    }

    public synchronized CheckIn update(String id, CheckInRequest request) {
        CheckIn old = get(id);
        Integer energy = request.energy() == null ? old.energy() : scale(request.energy());
        Integer mood = request.mood() == null ? old.mood() : scale(request.mood());
        Integer focusDesire = request.focusDesire() == null ? old.focusDesire() : scale(request.focusDesire());
        Integer fatigue = request.fatigue() == null ? old.fatigue() : scale(request.fatigue());
        CheckIn value = new CheckIn(id, energy, mood, focusDesire, fatigue,
                request.time() == null ? old.time() : request.time(),
                request.note() == null ? old.note() : clean(request.note()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.CHECK_IN_RECORDED, "check_in", id, copy.lifeCheckInTitle(),
                summary(value), value.time(), List.of("life", "check-in"), metadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.CHECK_IN_RECORDED, id);
    }

    private String summary(CheckIn value) {
        return copy.lifeCheckInSummary(value.energy(), value.mood(), value.focusDesire(), value.fatigue());
    }
    private Map<String, Object> metadata(CheckIn value) {
        return Map.of("energy", value.energy(), "mood", value.mood(),
                "focusDesire", value.focusDesire(), "fatigue", value.fatigue());
    }
    private int scale(Integer value) { return Math.max(1, Math.min(10, value)); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "状态记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
