package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.LifeEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

/** JSON persistence boundary for append-only LifeEvent history. */
@Repository
public final class LifeEventRepository {
    private static final String FILE_NAME = "life-events.json";
    private static final TypeReference<List<LifeEvent>> EVENTS = new TypeReference<>() { };
    private final JsonFileStore files;
    private final ObjectMapper mapper;

    public LifeEventRepository(JsonFileStore files, ObjectMapper mapper) { this.files = files; this.mapper = mapper; }

    public synchronized void append(LifeEvent event) {
        List<LifeEvent> events = new ArrayList<>(all());
        events.add(event);
        files.write(FILE_NAME, events);
    }

    public synchronized List<LifeEvent> recent(int limit) {
        return all().stream().sorted(Comparator.comparing(LifeEvent::occurredAt).reversed())
                .limit(Math.max(1, Math.min(limit, 100))).toList();
    }

    private List<LifeEvent> all() {
        if (!files.exists(FILE_NAME)) return List.of();
        try { return mapper.convertValue(files.read(FILE_NAME), EVENTS); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("???? LifeEvent ??", exception); }
    }
}
