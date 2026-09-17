package io.github.aomckin.lifehud.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpecialTask {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public String id; public String name; public String note = ""; public int coin; public int exp; public LocalDateTime createdTime; public int completedCount; public boolean done;
    public String dreamId = ""; public String goalId = ""; public String dreamMilestoneId = "";
    public boolean enabled = true;
    public static SpecialTask from(JsonNode n){var t=new SpecialTask();t.id=n.path("id").asText(DailyTask.makeId(n.path("name").asText()));t.name=n.path("name").asText();t.note=n.path("note").asText("");t.coin=n.path("coin").asInt();t.exp=n.path("exp").asInt(5);t.createdTime=n.has("created_time")?LocalDateTime.parse(n.path("created_time").asText(),FORMAT):LocalDateTime.now();t.completedCount=n.path("completed_count").asInt();t.done=n.path("done").asBoolean();t.dreamId=n.path("dreamId").asText("");t.goalId=n.path("goalId").asText("");t.dreamMilestoneId=n.path("dreamMilestoneId").asText("");t.enabled=n.path("enabled").asBoolean(true);return t;}
    public Map<String,Object> toMap(){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("name",name);m.put("note",note);m.put("coin",coin);m.put("exp",exp);m.put("created_time",createdTime.format(FORMAT));m.put("completed_count",completedCount);m.put("done",done);m.put("dreamId",dreamId);m.put("goalId",goalId);m.put("dreamMilestoneId",dreamMilestoneId);m.put("enabled",enabled);return m;}
}
