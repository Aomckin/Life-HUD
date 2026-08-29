package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.JournalEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class JournalEntryRepository {
    private static final String FILE="journal-entries.json";
    private static final TypeReference<List<JournalEntry>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public JournalEntryRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<JournalEntry> all(){
        if(!files.exists(FILE))return List.of();
        try{return mapper.convertValue(files.read(FILE),TYPE);}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取日记",e);}
    }
    public synchronized Optional<JournalEntry> find(String id){return all().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized JournalEntry save(JournalEntry value){List<JournalEntry> all=new ArrayList<>(all());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(FILE,all);return value;}
    public synchronized boolean remove(String id){List<JournalEntry> all=new ArrayList<>(all());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,all);return removed;}
}
