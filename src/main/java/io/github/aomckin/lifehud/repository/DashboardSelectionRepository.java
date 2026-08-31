package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.DashboardSelections;
import org.springframework.stereotype.Repository;

@Repository
public final class DashboardSelectionRepository {
    private static final String FILE="dashboard-selections.json";
    private final JsonFileStore files;private final ObjectMapper mapper;
    public DashboardSelectionRepository(JsonFileStore files,ObjectMapper mapper){this.files=files;this.mapper=mapper;}
    public synchronized DashboardSelections get(){if(!files.exists(FILE))return DashboardSelections.empty();try{return mapper.convertValue(files.read(FILE),DashboardSelections.class);}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取 Dashboard 展示选择",e);}}
    public synchronized DashboardSelections save(DashboardSelections value){files.write(FILE,value);return value;}
}
