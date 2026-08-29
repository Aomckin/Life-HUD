package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class GrowthAchievementService {
    private final Player player; private final GrowthRecordRepository records; private final LevelService levels;
    private final JsonFileStore files; private final GrowthStatsService stats;
    public GrowthAchievementService(Player player,GrowthRecordRepository records,LevelService levels,JsonFileStore files,GrowthStatsService stats){this.player=player;this.records=records;this.levels=levels;this.files=files;this.stats=stats;}
    public List<Map<String,Object>> all(){Map<String,java.time.Instant> times=unlockTimes();List<Map<String,Object>> result=new ArrayList<>(GrowthCatalog.ACHIEVEMENTS.stream().map(a->view(a,times.get(a.id()))).toList());Set<String> known=new HashSet<>();GrowthCatalog.ACHIEVEMENTS.forEach(v->known.add(v.id()));if(files.exists("achievements.json"))for(var node:files.read("achievements.json")){String id=node.path("id").asText();if(player.unlocked_achievements.contains(id)&&known.add(id)){Map<String,Object> legacy=new LinkedHashMap<>();legacy.put("id",id);legacy.put("name",node.path("name").asText(id));legacy.put("description",node.path("desc").asText("从旧版 Life HUD 保留下来的成长记录。"));legacy.put("category","LEGACY");legacy.put("hidden",false);legacy.put("unlocked",true);legacy.put("current",1);legacy.put("target",1);legacy.put("progress",100);legacy.put("unlockedAt",null);result.add(legacy);}}return result;}
    public Map<String,Object> find(String id){java.time.Instant time=unlockTimes().get(id);return GrowthCatalog.ACHIEVEMENTS.stream().filter(v->v.id().equals(id)).findFirst().map(a->view(a,time)).orElse(null);}
    private Map<String,Object> view(GrowthCatalog.AchievementRule a,java.time.Instant unlockedAt){boolean unlocked=player.unlocked_achievements.contains(a.id());long current=current(a.condition());long target=a.condition().target();Map<String,Object> map=new LinkedHashMap<>();map.put("id",a.id());map.put("name",a.hidden()&&!unlocked?"隐藏成就":a.name());map.put("description",a.hidden()&&!unlocked?"在生活里偶然遇见它。":a.description());map.put("category",a.category());map.put("icon",a.icon());map.put("conditionType",a.condition().type());map.put("hidden",a.hidden());map.put("unlocked",unlocked);map.put("current",Math.min(current,target));map.put("target",target);map.put("progress",target==0?100:Math.min(100,Math.round(current*100f/target)));map.put("unlockedAt",unlockedAt);return map;}
    private Map<String,java.time.Instant> unlockTimes(){Map<String,java.time.Instant> times=new HashMap<>();for(GrowthEventRecord record:records.all())for(String id:record.unlockedAchievements())times.merge(id,record.processedAt(),(a,b)->a.isBefore(b)?a:b);return times;}
    private long current(GrowthAchievementCondition condition){GrowthStats s=stats.current();return switch(condition.type()){case STREAK,CUSTOM->number(condition.metadata().get("current"));default->switch(condition.metric()){case IRON_FOCUS_COUNT->s.ironFocusCount();case POMODORO_FOCUS_COUNT->s.pomodoroFocusCount();case FOCUS_COUNT->s.focusCount();case EFFECTIVE_FOCUS_MINUTES->s.effectiveFocusMinutes();case MAX_EFFECTIVE_FOCUS_MINUTES->s.maxEffectiveFocusMinutes();case CROSS_MIDNIGHT_FOCUS_COUNT->s.crossMidnightFocusCount();case TASK_COUNT->s.taskCount();case LIFE_EVENT_COUNT->s.lifeEventCount();case MILESTONE_COUNT->s.milestoneCount();case LEVEL->levels.level(player.exp);case ENERGY->player.energy;};};}
    private long number(Object value){return value instanceof Number n?n.longValue():0;}
}
