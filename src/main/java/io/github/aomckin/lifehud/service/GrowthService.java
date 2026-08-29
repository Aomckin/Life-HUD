package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class GrowthService {
    private final Player player;private final LevelService levels;private final GrowthRecordRepository records;
    private final GrowthAchievementService achievements;private final GrowthTitleService titles;
    private final MilestoneService milestones;private final GrowthSnapshotService snapshots;private final GrowthEngine engine;
    public GrowthService(Player player,LevelService levels,GrowthRecordRepository records,GrowthAchievementService achievements,GrowthTitleService titles,MilestoneService milestones,GrowthSnapshotService snapshots,GrowthEngine engine){this.player=player;this.levels=levels;this.records=records;this.achievements=achievements;this.titles=titles;this.milestones=milestones;this.snapshots=snapshots;this.engine=engine;}
    public Map<String,Object> overview(){snapshots.captureToday();Map<String,Object> level=levels.status(player.exp);LocalDate today=LocalDate.now();ZoneId zone=ZoneId.systemDefault();int todayExp=records.all().stream().filter(v->v.processedAt().atZone(zone).toLocalDate().equals(today)).mapToInt(GrowthEventRecord::expDelta).sum();int todayEnergy=records.energy().stream().filter(v->v.occurredAt().atZone(zone).toLocalDate().equals(today)).mapToInt(EnergyRecord::delta).sum();List<Map<String,Object>> achievementViews=achievements.all();EnergyState energy=new EnergyState(player.energy,0,player.maxEnergy);Map<String,Object> map=new LinkedHashMap<>();map.put("version","0.4.0");map.put("level",level.get("level"));map.put("totalExp",player.exp);map.put("currentExp",level.get("current_exp"));map.put("requiredExp",level.get("required_exp"));map.put("expToNext",((Number)level.get("required_exp")).intValue()-((Number)level.get("current_exp")).intValue());map.put("energyState",energy);map.put("energy",energy.current());map.put("energyMin",energy.minimum());map.put("energyMax",energy.maximum());map.put("todayExpDelta",todayExp);map.put("todayEnergyDelta",todayEnergy);map.put("currentTitle",titles.currentName());map.put("achievementCount",achievementViews.stream().filter(v->Boolean.TRUE.equals(v.get("unlocked"))).count());map.put("achievementTotal",achievementViews.size());map.put("milestoneCount",milestones.all().size());map.put("titleCount",player.unlocked_titles.size());map.put("history",history(12));map.put("trend",snapshots.trend(7));return map;}
    public List<GrowthEventRecord> history(int limit){return records.all().stream().filter(this::visible).sorted(Comparator.comparing(GrowthEventRecord::processedAt).reversed()).limit(Math.max(1,Math.min(limit,100))).toList();}public List<EnergyRecord> energyHistory(){return records.energy();}public List<String> recalculate(){return engine.recalculate();}
    private boolean visible(GrowthEventRecord r){return r.expDelta()!=0||r.energyDelta()!=0||r.levelAfter()>r.levelBefore()||!r.unlockedAchievements().isEmpty()||!r.unlockedTitles().isEmpty();}
}
