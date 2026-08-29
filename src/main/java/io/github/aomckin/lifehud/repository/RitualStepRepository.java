package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.RitualStep;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public final class RitualStepRepository {
    private static final String FILE="ritual-steps.json";
    private static final TypeReference<List<RitualStep>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public RitualStepRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<RitualStep> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparingInt(RitualStep::sortOrder)).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取仪式步骤",e);}
    }
    public synchronized List<RitualStep> byRitual(String ritualId){return all().stream().filter(v->v.ritualId().equals(ritualId)).toList();}
    /** Replaces every step of one ritual with the given ordered list. */
    public synchronized void replaceRitualSteps(String ritualId,List<RitualStep> value){
        List<RitualStep> all=new ArrayList<>(all());all.removeIf(v->v.ritualId().equals(ritualId));all.addAll(value);files.write(FILE,all);
    }
    public synchronized void deleteByRitual(String ritualId){List<RitualStep> all=new ArrayList<>(all());if(all.removeIf(v->v.ritualId().equals(ritualId)))files.write(FILE,all);}
}
