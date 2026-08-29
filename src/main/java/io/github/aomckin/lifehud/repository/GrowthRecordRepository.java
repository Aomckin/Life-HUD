package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.EnergyRecord;
import io.github.aomckin.lifehud.domain.GrowthEventRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class GrowthRecordRepository {
    private static final String GROWTH = "growth-events.json";
    private static final String ENERGY = "energy-history.json";
    private static final TypeReference<List<GrowthEventRecord>> GROWTH_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<EnergyRecord>> ENERGY_TYPE = new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public GrowthRecordRepository(JsonFileStore files, ObjectMapper mapper) { this.files=files; this.mapper=mapper; }
    public synchronized Optional<GrowthEventRecord> find(String eventId) {
        return all().stream().filter(record -> record.eventId().equals(eventId)).findFirst();
    }
    public synchronized void append(GrowthEventRecord record) {
        List<GrowthEventRecord> values=new ArrayList<>(all()); values.add(record); files.write(GROWTH, values);
    }
    public synchronized List<GrowthEventRecord> all() { return read(GROWTH, GROWTH_TYPE); }
    public synchronized List<GrowthEventRecord> recent(int limit) {
        return all().stream().sorted(Comparator.comparing(GrowthEventRecord::processedAt).reversed())
                .limit(Math.max(1, Math.min(limit, 100))).toList();
    }
    public synchronized java.time.Instant achievementUnlockedAt(String id) {
        return all().stream().filter(record -> record.unlockedAchievements().contains(id))
                .map(GrowthEventRecord::processedAt).min(java.time.Instant::compareTo).orElse(null);
    }
    public synchronized void appendEnergy(EnergyRecord record) {
        List<EnergyRecord> values=new ArrayList<>(energy()); values.add(record); files.write(ENERGY, values);
    }
    public synchronized List<EnergyRecord> energy() { return read(ENERGY, ENERGY_TYPE); }
    private <T> List<T> read(String file, TypeReference<List<T>> type) {
        if (!files.exists(file)) return List.of();
        try { return mapper.convertValue(files.read(file), type); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("无法读取成长记录: " + file, exception); }
    }
}
