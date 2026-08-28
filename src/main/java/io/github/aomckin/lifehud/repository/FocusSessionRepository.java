package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.FocusSession;
import io.github.aomckin.lifehud.domain.FocusStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** JSON persistence boundary for Focus sessions. */
@Repository
public final class FocusSessionRepository {
    private static final String FILE_NAME = "focus-sessions.json";
    private static final TypeReference<List<FocusSession>> SESSIONS = new TypeReference<>() { };
    private final JsonFileStore files;
    private final ObjectMapper mapper;

    public FocusSessionRepository(JsonFileStore files, ObjectMapper mapper) { this.files = files; this.mapper = mapper; }

    public synchronized List<FocusSession> all() {
        if (!files.exists(FILE_NAME)) return List.of();
        try { return mapper.convertValue(files.read(FILE_NAME), SESSIONS); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("无法读取 Focus 会话", exception); }
    }

    public synchronized Optional<FocusSession> findById(String id) {
        return all().stream().filter(session -> session.id().equals(id)).findFirst();
    }

    public synchronized Optional<FocusSession> current() {
        return all().stream()
                .filter(session -> session.status() == FocusStatus.RUNNING || session.status() == FocusStatus.PAUSED)
                .max(Comparator.comparing(FocusSession::startedAt));
    }

    public synchronized void save(FocusSession session) {
        List<FocusSession> values = new ArrayList<>(all());
        values.removeIf(existing -> existing.id().equals(session.id()));
        values.add(session);
        files.write(FILE_NAME, values);
    }
}
