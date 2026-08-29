package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.EntertainmentRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lightweight real-life entry for Energy consumption: records what happened, then lets
 * {@link EnergyLedgerService} settle the actual spend and GrowthEngine settle the EXP.
 * energyCost is user-entered (no automatic per-minute algorithm); the actual consumed
 * amount may be smaller when Energy runs out — reality is recorded, not rejected.
 */
@Service
public final class EntertainmentRecordService {
    private final EntertainmentRecordRepository records;
    private final LifeEventService events;
    private final EnergyLedgerService ledger;
    private final GrowthCopy copy;

    public EntertainmentRecordService(EntertainmentRecordRepository records, LifeEventService events,
                                      EnergyLedgerService ledger, GrowthCopy copy) {
        this.records = records; this.events = events; this.ledger = ledger; this.copy = copy;
    }

    public List<EntertainmentRecord> all() { return records.all(); }

    public synchronized EntertainmentRecord create(EntertainmentRequest request) {
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("做了什么不能为空");
        int duration = nonNegative(request == null ? null : request.durationMinutes(), "持续时间");
        int cost = nonNegative(request == null ? null : request.energyCost(), "Energy 消耗");
        EntertainmentCategory category = EntertainmentCategory.from(request == null ? null : request.category());
        Instant now = Instant.now();
        Instant occurred = request == null || request.occurredAt() == null ? now : request.occurredAt();
        String id = UUID.randomUUID().toString();
        LifeEvent event = events.record(LifeEventType.ENTERTAINMENT_RECORDED, "entertainment", title,
                copy.entertainmentRecordedDescription(), List.of("entertainment"),
                Map.of("recordId", id, "sourceId", id, "category", category.name(),
                        "durationMinutes", duration, "energyCost", cost));
        // Reuse the unified ledger: GrowthEngine settles from the actually consumed Energy.
        if (cost > 0) ledger.spend(new EnergySpendRequest(cost, title, "entertainment", id));
        EntertainmentRecord value = new EntertainmentRecord(id, category, title, duration, cost,
                clean(request == null ? null : request.note()), occurred, event.id(), now, now);
        records.save(value);
        return value;
    }

    /** energyCost and occurredAt are settled facts and intentionally not editable. */
    public synchronized EntertainmentRecord update(String id, EntertainmentRequest request) {
        EntertainmentRecord old = records.find(id).orElseThrow(this::missing);
        String title = clean(request == null ? null : request.title());
        if (title.isBlank()) throw bad("做了什么不能为空");
        int duration = nonNegative(request == null ? null : request.durationMinutes(), "持续时间");
        EntertainmentCategory category = EntertainmentCategory.from(request == null ? null : request.category());
        EntertainmentRecord value = new EntertainmentRecord(old.id(), category, title, duration, old.energyCost(),
                clean(request == null ? null : request.note()), old.occurredAt(), old.lifeEventId(),
                old.createdAt(), Instant.now());
        records.save(value);
        events.record(LifeEventType.ENTERTAINMENT_UPDATED, "entertainment", value.title(),
                copy.entertainmentUpdatedDescription(), List.of("entertainment"), Map.of("recordId", id));
        return value;
    }

    /** Deletes the fact only; Energy / EXP history stays untouched (correct it via Energy Adjust if needed). */
    public synchronized void delete(String id) {
        EntertainmentRecord old = records.find(id).orElseThrow(this::missing);
        records.delete(id);
        events.record(LifeEventType.ENTERTAINMENT_DELETED, "entertainment", old.title(),
                copy.entertainmentDeletedDescription(), List.of("entertainment"), Map.of("recordId", id));
    }

    private int nonNegative(Integer value, String label) {
        int v = value == null ? 0 : value;
        if (v < 0) throw bad(label + "不能为负数");
        return v;
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "娱乐记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
