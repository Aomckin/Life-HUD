package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.EntertainmentRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class EntertainmentRecordRepository {
    private static final String FILE="entertainment-records.json";
    private static final TypeReference<List<EntertainmentRecord>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public EntertainmentRecordRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<EntertainmentRecord> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE).stream()
                .sorted(Comparator.comparing(EntertainmentRecord::occurredAt).reversed()).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取娱乐记录",e);}
    }
    public synchronized Optional<EntertainmentRecord> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(EntertainmentRecord value){List<EntertainmentRecord> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized boolean delete(String id){List<EntertainmentRecord> all=new ArrayList<>(all());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,all);return removed;}
}
