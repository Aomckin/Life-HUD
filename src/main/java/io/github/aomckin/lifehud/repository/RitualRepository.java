package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Ritual;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class RitualRepository {
    private static final String FILE="rituals.json";
    private static final TypeReference<List<Ritual>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public RitualRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<Ritual> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE);}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取仪式",e);}
    }
    public synchronized Optional<Ritual> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void save(Ritual value){List<Ritual> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);}
    public synchronized void delete(String id){List<Ritual> all=new ArrayList<>(all());if(all.removeIf(v->v.id().equals(id)))files.write(FILE,all);}
}
