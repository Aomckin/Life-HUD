package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Milestone;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class MilestoneRepository {
    private static final String FILE="milestones.json";
    private static final TypeReference<List<Milestone>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public MilestoneRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<Milestone> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparing(Milestone::pinned).reversed()
                        .thenComparing(Milestone::occurredAt, Comparator.reverseOrder())).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取里程碑",e);}
    }
    public synchronized Optional<Milestone> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(Milestone value){List<Milestone> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized boolean delete(String id){List<Milestone> all=new ArrayList<>(all());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,all);return removed;}
}
