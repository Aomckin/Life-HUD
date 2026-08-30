package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.LifeRecord;
import io.github.aomckin.lifehud.domain.LifeRecordRequest;
import io.github.aomckin.lifehud.domain.LifeRecordType;
import io.github.aomckin.lifehud.repository.LifeRecordRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic life observations. WATER and CAFFEINE differ only in presentation —
 * they share this one service, one repository and one API. A new niche record
 * becomes a CUSTOM label, never a new controller.
 */
@Service
public final class LifeRecordService {
    private final LifeRecordRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public LifeRecordService(LifeRecordRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<LifeRecord> all() { return records.all(); }
    public LifeRecord get(String id) { return records.find(id).orElseThrow(this::missing); }

    public synchronized LifeRecord create(LifeRecordRequest request) {
        LifeRecordType type = type(request == null ? null : request.type());
        if (request.value() == null || request.value() < 0) throw bad("需要一个不小于 0 的数值");
        Instant time = request.time() == null ? Instant.now() : request.time();
        String unit = request.unit() == null || request.unit().isBlank() ? defaultUnit(type) : request.unit().trim();
        Map<String, Object> metadata = normalizedMetadata(type, request.metadata());
        Instant now = Instant.now();
        LifeRecord value = new LifeRecord(UUID.randomUUID().toString(), type, request.value(), unit, time,
                clean(request.note()), metadata, images(request.images()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.LIFE_RECORDED, "life_record", value.id(), label(value),
                summary(value), time, List.of("life", "record"), displayMetadata(value));
        return value;
    }

    public synchronized LifeRecord update(String id, LifeRecordRequest request) {
        LifeRecord old = get(id);
        LifeRecordType type = request.type() == null ? old.type() : type(request.type());
        Double value2 = request.value() == null ? old.value() : request.value();
        if (value2 < 0) throw bad("需要一个不小于 0 的数值");
        Map<String, Object> metadata = request.metadata() == null ? old.metadata()
                : normalizedMetadata(type, request.metadata());
        LifeRecord value = new LifeRecord(id, type, value2,
                request.unit() == null ? old.unit() : request.unit().trim(),
                request.time() == null ? old.time() : request.time(),
                request.note() == null ? old.note() : clean(request.note()),
                metadata, request.images() == null ? old.images() : images(request.images()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.LIFE_RECORDED, "life_record", id, label(value),
                summary(value), value.time(), List.of("life", "record"), displayMetadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.LIFE_RECORDED, id);
    }

    private Map<String, Object> normalizedMetadata(LifeRecordType type, Map<String, Object> metadata) {
        Map<String, Object> merged = new HashMap<>(metadata == null ? Map.of() : metadata);
        if (type == LifeRecordType.CUSTOM) {
            String label = String.valueOf(merged.getOrDefault("label", "")).trim();
            if (label.isEmpty()) throw bad("自定义记录需要一个名称");
            merged.put("label", label);
        } else {
            merged.remove("label");
        }
        return merged;
    }

    private String label(LifeRecord value) {
        if (value.type() == LifeRecordType.CUSTOM)
            return String.valueOf(value.metadata().getOrDefault("label", copy.lifeRecordType("CUSTOM")));
        return copy.lifeRecordType(value.type().name());
    }

    private String summary(LifeRecord value) {
        String amount = value.value() % 1 == 0 ? String.valueOf((long) value.value()) : String.valueOf(value.value());
        return label(value) + " " + amount + value.unit();
    }
    private Map<String, Object> displayMetadata(LifeRecord value) {
        Map<String, Object> display = new HashMap<>(value.metadata());
        display.put("value", value.value());
        display.put("unit", value.unit());
        if (!value.images().isEmpty()) display.put("images", value.images());
        return display;
    }
    private List<String> images(List<String> images) { return images == null ? List.of() : images.stream()
            .filter(path -> path != null && !path.isBlank()).map(String::trim).toList(); }
    private String defaultUnit(LifeRecordType type) {
        return switch (type) {
            case WATER, ALCOHOL -> "ml";
            case CAFFEINE -> "杯";
            case SUNLIGHT, OUTDOOR -> "min";
            case SOCIAL -> "次";
            case BODY_STATUS -> "级";
            case CUSTOM -> "";
        };
    }
    private LifeRecordType type(String type) {
        try { return LifeRecordType.valueOf(type == null ? "" : type.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { throw bad("未知的生活记录类型"); }
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "生活记录不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
