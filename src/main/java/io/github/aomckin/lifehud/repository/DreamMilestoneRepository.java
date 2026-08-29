package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.DreamMilestone;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class DreamMilestoneRepository {
    private static final String FILE="dream-milestones.json";
    private static final TypeReference<List<DreamMilestone>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public DreamMilestoneRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<DreamMilestone> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparing(DreamMilestone::sortOrder).thenComparing(DreamMilestone::createdAt)).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取梦想里程碑",e);}
    }
    public synchronized List<DreamMilestone> byGoal(String goalId){return all().stream().filter(v->v.goalId().equals(goalId)).toList();}
    public synchronized Optional<DreamMilestone> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(DreamMilestone value){List<DreamMilestone> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized void delete(String id){List<DreamMilestone> all=new ArrayList<>(all());if(all.removeIf(v->v.id().equals(id)))files.write(FILE,all);}
}
