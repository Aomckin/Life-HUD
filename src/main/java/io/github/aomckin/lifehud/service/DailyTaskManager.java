package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.domain.DailyTask;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

public final class DailyTaskManager {
    private final JsonFileStore repo; private final Supplier<LocalDate> today; private final Random random; private final ObjectNode data;
    private final List<DailyTask> allTasks=new ArrayList<>(),tasks=new ArrayList<>();
    public DailyTaskManager(JsonFileStore repo){this(repo,LocalDate::now,new Random());}
    public DailyTaskManager(JsonFileStore repo,Supplier<LocalDate> today,Random random){this.repo=repo;this.today=today;this.random=random;data=(ObjectNode)repo.read("tasks.json");for(JsonNode n:data.path("tasks"))allTasks.add(DailyTask.from(n));refresh();setActive();}
    public void refresh(){String date=today.get().toString();if(!date.equals(data.path("last_update_date").asText())){allTasks.forEach(t->t.done=false);data.put("last_update_date",date);draw();save();return;}if(!hasValidActiveTasks()){draw();save();}}
    public void draw(){List<String> ids=unfinishedTaskIds();Collections.shuffle(ids,random);data.putArray("active_task_ids").addAll(ids.stream().limit(Math.min(3,ids.size())).map(com.fasterxml.jackson.databind.node.TextNode::new).toList());}
    public boolean hasValidActiveTasks(){JsonNode ids=data.path("active_task_ids");List<String> unfinished=unfinishedTaskIds();if(!ids.isArray()||ids.size()!=Math.min(3,unfinished.size()))return false;Set<String> valid=new HashSet<>(unfinished);Set<String> active=new HashSet<>();for(JsonNode id:ids)if(!valid.contains(id.asText())||!active.add(id.asText()))return false;return true;}
    public void setActive(){tasks.clear();Map<String,DailyTask> by=new LinkedHashMap<>();allTasks.forEach(t->by.put(t.id,t));for(JsonNode id:data.path("active_task_ids"))if(by.containsKey(id.asText()))tasks.add(by.get(id.asText()));}
    public int[] finish(int index){if(index<0||index>=tasks.size())throw new IndexOutOfBoundsException(index);DailyTask t=tasks.get(index);if(t.done)return new int[]{0,0};t.done=true;t.completedCount++;save();return new int[]{t.reward,t.exp};}
    public void redraw(){draw();save();setActive();}
    public void save(){data.set("tasks",repoNode(allTasks.stream().map(DailyTask::toMap).toList()));repo.write("tasks.json",data);}
    private List<String> unfinishedTaskIds(){return allTasks.stream().filter(t->!t.done).map(t->t.id).collect(java.util.stream.Collectors.toCollection(ArrayList::new));}
    private com.fasterxml.jackson.databind.node.ArrayNode repoNode(Object o){return (com.fasterxml.jackson.databind.node.ArrayNode)new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(o);}
    public List<DailyTask> allTasks(){return Collections.unmodifiableList(allTasks);}public List<DailyTask> tasks(){return Collections.unmodifiableList(tasks);}public DailyTask task(){return tasks.isEmpty()?null:tasks.getFirst();}
}
