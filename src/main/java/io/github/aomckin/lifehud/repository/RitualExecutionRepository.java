package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.RitualExecution;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class RitualExecutionRepository {
    private static final String FILE="ritual-executions.json";
    private static final TypeReference<List<RitualExecution>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public RitualExecutionRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<RitualExecution> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparing(RitualExecution::startedAt).reversed()).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取仪式执行记录",e);}
    }
    public synchronized List<RitualExecution> byRitual(String ritualId){return all().stream().filter(v->v.ritualId().equals(ritualId)).toList();}
    public synchronized Optional<RitualExecution> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(RitualExecution value){List<RitualExecution> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
}
