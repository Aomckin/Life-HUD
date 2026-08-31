package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.DashboardHeadline;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

@Repository
public final class DashboardHeadlineRepository {
    private static final String FILE="dashboard-headlines.json";
    private static final String DEFAULTS="data/content/dashboard-headlines.json";
    private static final TypeReference<List<DashboardHeadline>> TYPE=new TypeReference<>(){};
    private final JsonFileStore files;private final ObjectMapper mapper;
    public DashboardHeadlineRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized List<DashboardHeadline> all(){
        LinkedHashMap<String,DashboardHeadline> merged=new LinkedHashMap<>();
        defaults().forEach(value->merged.put(value.text(),value));
        custom().forEach(value->merged.putIfAbsent(value.text(),value));
        return List.copyOf(merged.values());
    }
    public synchronized DashboardHeadline save(DashboardHeadline value){List<DashboardHeadline> values=new ArrayList<>(custom());values.add(value);files.write(FILE,values);return value;}
    public synchronized boolean remove(String id){List<DashboardHeadline> values=new ArrayList<>(custom());boolean removed=values.removeIf(v->v.id().equals(id));if(removed)files.write(FILE,values);return removed;}
    private List<DashboardHeadline> custom(){if(!files.exists(FILE))return List.of();try{return List.copyOf(mapper.convertValue(files.read(FILE),TYPE));}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取 Dashboard 自定义文案",e);}}
    private List<DashboardHeadline> defaults(){try(var input=new ClassPathResource(DEFAULTS).getInputStream()){return mapper.readValue(input,TYPE);}catch(IOException e){throw new IllegalStateException("无法读取 Dashboard 默认文案",e);}}
}
