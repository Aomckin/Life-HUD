package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.CheckIn;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class CheckInRepository {
    private static final String FILE="check-ins.json";
    private static final TypeReference<List<CheckIn>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public CheckInRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<CheckIn> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE);}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取状态记录",e);}
    }
    public synchronized Optional<CheckIn> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized CheckIn save(CheckIn value){List<CheckIn> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);return value;}
    public synchronized boolean remove(String id){List<CheckIn> all=new ArrayList<>(all());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,all);return removed;}
}
