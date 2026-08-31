package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.DashboardHeadline;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public final class DashboardHeadlineRepository {
    private static final String FILE="dashboard-headlines.json";
    private static final TypeReference<List<DashboardHeadline>> TYPE=new TypeReference<>(){};
    private final JsonFileStore files;private final ObjectMapper mapper;
    public DashboardHeadlineRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<DashboardHeadline> all(){if(!files.exists(FILE))return List.of();try{return List.copyOf(mapper.convertValue(files.read(FILE),TYPE));}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取 Dashboard 文案",e);}}
    public synchronized DashboardHeadline save(DashboardHeadline value){List<DashboardHeadline> values=new ArrayList<>(all());values.add(value);files.write(FILE,values);return value;}
    public synchronized boolean remove(String id){List<DashboardHeadline> values=new ArrayList<>(all());boolean removed=values.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,values);return removed;}
}
