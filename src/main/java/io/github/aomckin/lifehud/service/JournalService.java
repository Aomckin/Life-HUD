package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.JournalEntry;
import io.github.aomckin.lifehud.domain.JournalRequest;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.JournalEntryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Manual journal entries; plain text, backfillable, editable, removable. */
@Service
public final class JournalService {
    private final JournalEntryRepository records;
    private final LifeFactRecorder facts;
    private final GrowthCopy copy;

    public JournalService(JournalEntryRepository records, LifeFactRecorder facts, GrowthCopy copy) {
        this.records = records; this.facts = facts; this.copy = copy;
    }

    public List<JournalEntry> all() { return records.all(); }
    public JournalEntry get(String id) { return records.find(id).orElseThrow(this::missing); }

    public synchronized JournalEntry create(JournalRequest request) {
        String content = clean(request == null ? null : request.content());
        if (content.isEmpty()) throw bad("日记内容不能为空");
        Instant occurredAt = request == null || request.occurredAt() == null ? Instant.now() : request.occurredAt();
        Instant now = Instant.now();
        JournalEntry value = new JournalEntry(UUID.randomUUID().toString(), content, occurredAt,
                images(request == null ? null : request.images()), tags(request == null ? null : request.tags()), now, now);
        records.save(value);
        facts.recordCreated(LifeEventType.JOURNAL_WRITTEN, "journal", value.id(), copy.lifeJournalTitle(),
                content, occurredAt, List.of("life", "journal"), metadata(value));
        return value;
    }

    public synchronized JournalEntry update(String id, JournalRequest request) {
        JournalEntry old = get(id);
        String content = request.content() == null ? old.content() : clean(request.content());
        if (content.isEmpty()) throw bad("日记内容不能为空");
        JournalEntry value = new JournalEntry(id, content,
                request.occurredAt() == null ? old.occurredAt() : request.occurredAt(),
                request.images() == null ? old.images() : images(request.images()),
                request.tags() == null ? old.tags() : tags(request.tags()),
                old.createdAt(), Instant.now());
        records.save(value);
        facts.recordUpdated(LifeEventType.JOURNAL_WRITTEN, "journal", id, copy.lifeJournalTitle(),
                content, value.occurredAt(), List.of("life", "journal"), metadata(value));
        return value;
    }

    public synchronized void remove(String id) {
        get(id);
        records.remove(id);
        facts.recordDeleted(LifeEventType.JOURNAL_WRITTEN, id);
    }

    private List<String> images(List<String> images) {
        if (images == null) return List.of();
        return images.stream().filter(path -> path != null && !path.isBlank()).map(String::trim).toList();
    }
    private List<String> tags(List<String> tags) {
        if (tags == null) return List.of();
        return tags.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::trim).toList();
    }
    private Map<String, Object> metadata(JournalEntry value) {
        Map<String, Object> metadata = new HashMap<>();
        if (!value.images().isEmpty()) metadata.put("images", value.images());
        if (!value.tags().isEmpty()) metadata.put("entryTags", value.tags());
        return metadata;
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "日记不存在"); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
