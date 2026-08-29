package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Goal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class GoalRepository {
    private static final String FILE="goals.json";
    private static final TypeReference<List<Goal>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public GoalRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<Goal> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparing(Goal::sortOrder).thenComparing(Goal::createdAt)).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取目标",e);}
    }
    public synchronized List<Goal> byDream(String dreamId){return all().stream().filter(v->v.dreamId().equals(dreamId)).toList();}
    public synchronized Optional<Goal> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(Goal value){List<Goal> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized void delete(String id){List<Goal> all=new ArrayList<>(all());if(all.removeIf(v->v.id().equals(id)))files.write(FILE,all);}
}
