package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.GrowthStats;
import org.springframework.stereotype.Repository;

@Repository
public final class GrowthStatsRepository {
    private static final String FILE = "growth-stats.json";
    private final JsonFileStore files;
    private final ObjectMapper mapper;

    public GrowthStatsRepository(JsonFileStore files, ObjectMapper mapper) {
        this.files = files;
        this.mapper = mapper;
    }

    public synchronized boolean exists() { return files.exists(FILE); }
    public synchronized GrowthStats load() {
        return exists() ? mapper.convertValue(files.read(FILE), GrowthStats.class) : GrowthStats.empty();
    }
    public synchronized void save(GrowthStats stats) { files.write(FILE, stats); }
}
