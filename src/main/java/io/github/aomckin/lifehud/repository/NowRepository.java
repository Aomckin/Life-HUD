package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.NowSnapshot;
import io.github.aomckin.lifehud.domain.NowState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public final class NowRepository {
    private static final String STATE="now-state.json";
    private static final String SNAPSHOTS="now-snapshots.json";
    private static final TypeReference<List<NowSnapshot>> TYPE=new TypeReference<>() { };
    private final JsonFileStore files; private final ObjectMapper mapper;
    public NowRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}

    public synchronized Optional<NowState> state(){
        if(!files.exists(STATE))return Optional.empty();
        try{return Optional.ofNullable(mapper.convertValue(files.read(STATE),NowState.class));}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取「现在。」状态",e);}
    }
    public synchronized void saveState(NowState value){files.write(STATE,value);}

    public synchronized List<NowSnapshot> snapshots(){
        if(!files.exists(SNAPSHOTS))return List.of();
        try{return mapper.convertValue(files.read(SNAPSHOTS),TYPE).stream()
                .sorted(Comparator.comparing(NowSnapshot::createdAt).reversed()).toList();}
        catch(IllegalArgumentException e){throw new IllegalStateException("无法读取「现在。」快照",e);}
    }
    public synchronized Optional<NowSnapshot> findSnapshot(String id){return snapshots().stream().filter(v->v.id().equals(id)).findFirst();}
    public synchronized void saveSnapshot(NowSnapshot value){List<NowSnapshot> all=new ArrayList<>(snapshots());all.removeIf(v->v.id().equals(value.id()));all.add(value);files.write(SNAPSHOTS,all);}
    public synchronized boolean deleteSnapshot(String id){List<NowSnapshot> all=new ArrayList<>(snapshots());boolean removed=all.removeIf(v->v.id().equals(id));if(removed)files.write(SNAPSHOTS,all);return removed;}
}
