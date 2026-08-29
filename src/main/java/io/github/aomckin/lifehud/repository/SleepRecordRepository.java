package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.SleepRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class SleepRecordRepository {
    private static final String FILE="sleep-records.json";
    private static final TypeReference<List<SleepRecord>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public SleepRecordRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<SleepRecord> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE);}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取睡眠记录",e);}
    }
    public synchronized Optional<SleepRecord> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized SleepRecord save(SleepRecord value){List<SleepRecord> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);return value;}
    public synchronized boolean remove(String id){List<SleepRecord> all=new ArrayList<>(all());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,all);return removed;}
}
