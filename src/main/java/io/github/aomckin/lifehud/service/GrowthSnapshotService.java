package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class GrowthSnapshotService {
    private final GrowthSnapshotRepository snapshots;private final Player player;private final LevelService levels;
    private final GrowthStatsService stats;
    public GrowthSnapshotService(GrowthSnapshotRepository snapshots,Player player,LevelService levels,GrowthStatsService stats){this.snapshots=snapshots;this.player=player;this.levels=levels;this.stats=stats;}
    public synchronized GrowthSnapshot captureToday(){Instant now=Instant.now();GrowthStats s=stats.current();GrowthSnapshot value=new GrowthSnapshot(LocalDate.now(),levels.level(player.exp),player.exp,player.energy,s.actualFocusMinutes(),s.effectiveFocusMinutes(),(int)s.taskCount(),(int)s.lifeEventCount(),player.unlocked_achievements.size(),(int)s.milestoneCount(),now);snapshots.upsert(value);return value;}
    public List<GrowthSnapshot> all(){captureToday();return snapshots.all();}
    public List<GrowthSnapshot> recent(int days){LocalDate from=LocalDate.now().minusDays(Math.max(1,Math.min(days,365))-1L);return all().stream().filter(v->!v.date().isBefore(from)).toList();}
    public List<GrowthTrendPoint> trend(int days){List<GrowthSnapshot> values=all();LocalDate from=LocalDate.now().minusDays(Math.max(1,Math.min(days,365))-1L);List<GrowthTrendPoint> result=new java.util.ArrayList<>();for(int i=0;i<values.size();i++){GrowthSnapshot current=values.get(i);if(current.date().isBefore(from))continue;GrowthSnapshot previous=i==0?null:values.get(i-1);long focus=previous==null?current.totalEffectiveFocusMinutes():Math.max(0,current.totalEffectiveFocusMinutes()-previous.totalEffectiveFocusMinutes());int exp=previous==null?current.totalExp():Math.max(0,current.totalExp()-previous.totalExp());int tasks=previous==null?current.totalTaskCompleted():Math.max(0,current.totalTaskCompleted()-previous.totalTaskCompleted());result.add(new GrowthTrendPoint(current.date(),focus,exp,tasks,current.energy(),current.achievementCount(),current.milestoneCount()));}return List.copyOf(result);}
}
