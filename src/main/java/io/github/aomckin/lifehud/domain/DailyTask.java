package io.github.aomckin.lifehud.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DailyTask {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public String id; public String name; public int reward; public int exp; public LocalDateTime createdTime; public int completedCount; public boolean done;
    public static DailyTask from(JsonNode n){var t=new DailyTask();t.id=n.path("id").asText(makeId(n.path("name").asText()));t.name=n.path("name").asText();t.reward=n.path("reward").asInt();t.exp=n.path("exp").asInt(5);t.createdTime=n.has("created_time")?LocalDateTime.parse(n.path("created_time").asText(),FORMAT):LocalDateTime.now();t.completedCount=n.path("completed_count").asInt();t.done=n.path("done").asBoolean();return t;}
    public Map<String,Object> toMap(){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("name",name);m.put("reward",reward);m.put("exp",exp);m.put("created_time",createdTime.format(FORMAT));m.put("completed_count",completedCount);m.put("done",done);return m;}
    public static String makeId(String name){return name.toLowerCase().replace(" ","_");}
}
