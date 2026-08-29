package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.EnergyAdjustRequest;
import io.github.aomckin.lifehud.domain.EnergyChangeType;
import io.github.aomckin.lifehud.domain.EnergySpendRequest;
import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.GrowthRecordRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unified Energy ledger for current and future modules (Media / Life / Ritual ...).
 * Every change is recorded as a LifeEvent so GrowthEngine stays the single settlement
 * point; the ledger only classifies the change and reports the durable receipt.
 */
@Service
public final class EnergyLedgerService {
    private final LifeEventService events;
    private final GrowthRecordRepository receipts;

    public EnergyLedgerService(LifeEventService events, GrowthRecordRepository receipts) {
        this.events = events; this.receipts = receipts;
    }

    /** Real Energy consumption by a life activity. GrowthEngine settles EXP from the actually spent amount. */
    public Map<String, Object> spend(EnergySpendRequest request) {
        int amount = request == null ? 0 : request.amount();
        if (amount <= 0) throw bad("消费量必须大于 0");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("changeType", EnergyChangeType.SPEND.name());
        metadata.put("requestedEnergy", amount);
        if (request.sourceId() != null && !request.sourceId().isBlank()) metadata.put("sourceId", request.sourceId().trim());
        LifeEvent event = events.record(LifeEventType.ENERGY_SPENT, clean(request.sourceType(), "manual"),
                clean(request.reason(), "Energy 消费"), "请求消费 " + amount + " Energy",
                List.of("growth", "energy"), metadata);
        return receipt(event.id(), -amount);
    }

    /** Manual correction of the Energy value; classified ADJUST and never yields EXP. */
    public Map<String, Object> adjust(EnergyAdjustRequest request) {
        int delta = request == null ? 0 : request.delta();
        if (delta == 0) throw bad("调整量不能为 0");
        LifeEvent event = events.record(LifeEventType.ENERGY_CHANGED, "system", clean(request.reason(), "Energy 调整"),
                "Energy 调整 " + (delta > 0 ? "+" : "") + delta, List.of("growth", "energy"),
                Map.of("changeType", EnergyChangeType.ADJUST.name(), "energyDelta", delta));
        return receipt(event.id(), delta);
    }

    /** Natural decay entry for future schedulers; classified DECAY and never yields EXP. */
    public Map<String, Object> decay(int amount, String reason) {
        int value = Math.max(0, amount);
        if (value == 0) throw bad("衰减量必须大于 0");
        LifeEvent event = events.record(LifeEventType.ENERGY_CHANGED, "system", clean(reason, "Energy 衰减"),
                "Energy 自然衰减 " + value, List.of("growth", "energy"),
                Map.of("changeType", EnergyChangeType.DECAY.name(), "energyDelta", -value));
        return receipt(event.id(), -value);
    }

    private Map<String, Object> receipt(String eventId, int requestedDelta) {
        return receipts.find(eventId)
                .map(record -> Map.<String, Object>of("eventId", eventId, "requestedDelta", requestedDelta,
                        "actualDelta", record.energyDelta(), "expGained", record.expDelta()))
                .orElseThrow(() -> new IllegalStateException("成长回执缺失: " + eventId));
    }

    private String clean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
