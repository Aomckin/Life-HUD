package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.time.Instant;
import java.util.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/** Applies LifeEvents to the durable growth layer with event-id idempotency. */
@Service
public final class GrowthEngine {
    private final GrowthRules rules; private final GrowthRecordRepository records; private final Player player;
    private final PlayerService playerService; private final PlayerRepository players; private final LevelService levels;
    private final LifeEventService events; private final GrowthStatsService stats;
    public GrowthEngine(GrowthRules rules,GrowthRecordRepository records,Player player,PlayerService playerService,
                        PlayerRepository players,LevelService levels,LifeEventService events,GrowthStatsService stats){
        this.rules=rules;this.records=records;this.player=player;this.playerService=playerService;this.players=players;
        this.levels=levels;this.events=events;this.stats=stats;
    }
    /** Migration hook: recognize facts that predate v0.4 without replaying rewards. */
    @PostConstruct public void migrateExistingFacts(){GrowthStats existing=stats.current();List<String> achievements=unlockAchievements(existing);List<String> titles=unlockTitles(achievements);if(!achievements.isEmpty()||!titles.isEmpty())players.save(player);}
    public synchronized GrowthResult process(LifeEvent event) {
        Optional<GrowthEventRecord> previous=records.find(event.id());
        if(previous.isPresent()){GrowthEventRecord r=previous.get();return result(r,true);}
        GrowthRules.Change change=rules.evaluate(event); int beforeLevel=levels.level(player.exp); int beforeEnergy=player.energy;
        if(change.exp()>0) playerService.addExp(player,change.exp());
        if(change.energy()!=0) playerService.addEnergy(player,change.energy());
        if(event.type()==LifeEventType.TASK_COMPLETED){
            if("special".equalsIgnoreCase(String.valueOf(event.metadata().get("taskSource")))) playerService.incrementSpecialTaskDone(player);
            else playerService.incrementTaskDone(player);
        }
        GrowthStats currentStats=stats.apply(event);int afterLevel=levels.level(player.exp); List<String> achievements=unlockAchievements(currentStats); List<String> titles=unlockTitles(achievements);
        players.save(player);
        if(player.energy!=beforeEnergy) records.appendEnergy(new EnergyRecord(UUID.randomUUID().toString(),player.energy-beforeEnergy,
                beforeEnergy,player.energy,change.reason(),event.id(),event.occurredAt()));
        GrowthEventRecord receipt=new GrowthEventRecord(event.id(),Instant.now(),change.exp(),player.energy-beforeEnergy,
                beforeLevel,afterLevel,change.reason(),achievements,titles);
        records.append(receipt);
        if(afterLevel>beforeLevel) events.recordDerived(LifeEventType.LEVEL_UP,"growth","Level Up",
                "Lv."+beforeLevel+" → Lv."+afterLevel,List.of("growth"),Map.of("oldLevel",beforeLevel,"newLevel",afterLevel,"sourceEventId",event.id()));
        for(String id:achievements){GrowthCatalog.AchievementRule a=achievement(id);events.recordDerived(LifeEventType.ACHIEVEMENT_UNLOCKED,
                "achievement",a.name(),a.description(),List.of("growth","achievement"),Map.of("achievementId",id,"sourceEventId",event.id()));}
        for(String id:titles){GrowthCatalog.TitleDefinition t=title(id);events.recordDerived(LifeEventType.TITLE_UNLOCKED,"title",t.name(),
                t.description(),List.of("growth","title"),Map.of("titleId",id,"sourceEventId",event.id()));}
        return result(receipt,false);
    }
    public synchronized void recordDerivedEvent(){stats.recordDerivedEvent();}
    public synchronized List<String> recalculate(){GrowthStats rebuilt=stats.rebuild();List<String> achievements=unlockAchievements(rebuilt);List<String> titles=unlockTitles(achievements);if(!achievements.isEmpty()||!titles.isEmpty())players.save(player);for(String id:achievements){var a=achievement(id);events.recordDerived(LifeEventType.ACHIEVEMENT_UNLOCKED,"achievement",a.name(),a.description(),List.of("growth","achievement"),Map.of("achievementId",id,"sourceEventId","recalculate"));}for(String id:titles){var t=title(id);events.recordDerived(LifeEventType.TITLE_UNLOCKED,"title",t.name(),t.description(),List.of("growth","title"),Map.of("titleId",id,"sourceEventId","recalculate"));}return achievements;}
    private List<String> unlockAchievements(GrowthStats values){List<String> unlocked=new ArrayList<>();for(var rule:GrowthCatalog.ACHIEVEMENTS){if(!player.unlocked_achievements.contains(rule.id())&&metric(values,rule.condition().metric())>=rule.condition().target()){playerService.unlockAchievement(player,rule.id());unlocked.add(rule.id());}}return unlocked;}
    private List<String> unlockTitles(List<String> achievementIds){List<String> unlocked=new ArrayList<>();for(var definition:GrowthCatalog.TITLES){if(achievementIds.contains(definition.sourceId())&&!player.unlocked_titles.contains(definition.id())){playerService.unlockTitle(player,definition.id());unlocked.add(definition.id());}}if(player.equipped_title.isBlank()&&!unlocked.isEmpty())playerService.equipTitle(player,unlocked.getFirst());return unlocked;}
    private long metric(GrowthStats s,GrowthMetric m){return switch(m){case IRON_FOCUS_COUNT->s.ironFocusCount();case POMODORO_FOCUS_COUNT->s.pomodoroFocusCount();case FOCUS_COUNT->s.focusCount();case EFFECTIVE_FOCUS_MINUTES->s.effectiveFocusMinutes();case MAX_EFFECTIVE_FOCUS_MINUTES->s.maxEffectiveFocusMinutes();case CROSS_MIDNIGHT_FOCUS_COUNT->s.crossMidnightFocusCount();case TASK_COUNT->s.taskCount();case LIFE_EVENT_COUNT->s.lifeEventCount();case MILESTONE_COUNT->s.milestoneCount();case LEVEL->levels.level(player.exp);case ENERGY->player.energy;};}
    private GrowthResult result(GrowthEventRecord r,boolean duplicate){return new GrowthResult(r.eventId(),r.expDelta(),r.energyDelta(),r.levelBefore(),r.levelAfter(),r.levelAfter()>r.levelBefore(),r.unlockedAchievements(),r.unlockedTitles(),r.result(),duplicate);}
    private GrowthCatalog.AchievementRule achievement(String id){return GrowthCatalog.ACHIEVEMENTS.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();}
    private GrowthCatalog.TitleDefinition title(String id){return GrowthCatalog.TITLES.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();}
}
