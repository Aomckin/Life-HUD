package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.domain.SpecialTask;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

public final class SpecialTaskManager {
    private final JsonFileStore repo;private final Supplier<LocalDate> today;private final Random random;private final ObjectNode data;private int slotCount;
    private final List<SpecialTask> allTasks=new ArrayList<>(),tasks=new ArrayList<>();
    public SpecialTaskManager(JsonFileStore repo,int slots){this(repo,slots,LocalDate::now,new Random());}
    public SpecialTaskManager(JsonFileStore repo,int slots,Supplier<LocalDate> today,Random random){this.repo=repo;this.slotCount=Math.max(1,slots);this.today=today;this.random=random;data=(ObjectNode)repo.read("special_tasks.json");if(!data.has("active_task_ids")){var a=data.putArray("active_task_ids");if(!data.path("active_task_id").asText().isBlank())a.add(data.path("active_task_id").asText());}for(JsonNode n:data.path("tasks"))allTasks.add(SpecialTask.from(n));refresh();setActive();}
    public void refresh(){String date=today.get().toString();if(!date.equals(data.path("last_update_date").asText())){allTasks.forEach(t->t.done=false);data.put("last_update_date",date);draw();save();return;}if(!hasValidActiveTasks()){fill();save();}}
    public void draw(){List<String> ids=allTasks.stream().map(t->t.id).collect(java.util.stream.Collectors.toCollection(ArrayList::new));Collections.shuffle(ids,random);var a=data.putArray("active_task_ids");ids.stream().limit(Math.min(slotCount,ids.size())).forEach(a::add);}
    public void fill(){List<String> valid=allTasks.stream().map(t->t.id).toList(),active=new ArrayList<>();for(JsonNode id:data.path("active_task_ids"))if(valid.contains(id.asText())&&!active.contains(id.asText()))active.add(id.asText());int count=Math.min(slotCount,valid.size());if(active.size()>count)active=new ArrayList<>(active.subList(0,count));List<String> rest=new ArrayList<>(valid);rest.removeAll(active);Collections.shuffle(rest,random);active.addAll(rest.stream().limit(count-active.size()).toList());var a=data.putArray("active_task_ids");active.forEach(a::add);}
    public boolean hasValidActiveTasks(){JsonNode ids=data.path("active_task_ids");if(!ids.isArray()||ids.size()!=Math.min(slotCount,allTasks.size()))return false;Set<String> valid=new HashSet<>();allTasks.forEach(t->valid.add(t.id));for(JsonNode id:ids)if(!valid.contains(id.asText()))return false;return true;}
    public void setActive(){tasks.clear();Map<String,SpecialTask> by=new LinkedHashMap<>();allTasks.forEach(t->by.put(t.id,t));for(JsonNode id:data.path("active_task_ids"))if(by.containsKey(id.asText()))tasks.add(by.get(id.asText()));}
    public int[] finish(int index){if(index<0||index>=tasks.size())return new int[]{0,0};SpecialTask t=tasks.get(index);if(t.done)return new int[]{0,0};t.done=true;t.completedCount++;save();return new int[]{t.coin,t.exp};}
    /** Completes any special task by id; finishing a done task is a no-op. */
    public int[] finishById(String id){SpecialTask t=allTasks.stream().filter(v->v.id.equals(id)).findFirst().orElse(null);if(t==null||t.done)return new int[]{0,0};t.done=true;t.completedCount++;save();return new int[]{0,t.exp};}
    public void setSlotCount(int slots){slotCount=Math.max(1,slots);if(!hasValidActiveTasks()){fill();save();}setActive();}
    public void save(){data.set("tasks",new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(allTasks.stream().map(SpecialTask::toMap).toList()));repo.write("special_tasks.json",data);}
    public List<SpecialTask> allTasks(){return Collections.unmodifiableList(allTasks);}public List<SpecialTask> tasks(){return Collections.unmodifiableList(tasks);}public SpecialTask task(){return tasks.isEmpty()?null:tasks.getFirst();}public int slotCount(){return slotCount;}
}
