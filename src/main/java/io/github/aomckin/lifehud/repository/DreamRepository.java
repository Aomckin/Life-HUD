package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Dream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class DreamRepository {
    private static final String FILE="dreams.json";
    private static final TypeReference<List<Dream>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public DreamRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<Dream> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE);}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取梦想",e);}
    }
    public synchronized Optional<Dream> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(Dream value){List<Dream> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized void remove(String id){List<Dream> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(id));files.write(FILE,all);}
}
