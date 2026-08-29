package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.GrowthSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public final class GrowthSnapshotRepository {
    private static final String FILE="growth-snapshots.json";
    private static final TypeReference<List<GrowthSnapshot>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public GrowthSnapshotRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<GrowthSnapshot> all(){if(!files.exists(FILE))return List.of();try{return mapper.convertValue(files.read(FILE),TYPE).stream().sorted(Comparator.comparing(GrowthSnapshot::date)).toList();}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取成长快照",e);}}
    public synchronized void upsert(GrowthSnapshot value){List<GrowthSnapshot> all=new ArrayList<>(all());all.removeIf(v->v.date().equals(value.date()));all.add(value);files.write(FILE,all);}
}
