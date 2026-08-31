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
    private final GrowthCatalog catalog; private final GrowthCopy copy;
    private final AchievementEvaluator evaluator;
    public GrowthEngine(GrowthRules rules,GrowthRecordRepository records,Player player,PlayerService playerService,
                        PlayerRepository players,LevelService levels,LifeEventService events,GrowthStatsService stats,
                        GrowthCatalog catalog,GrowthCopy copy,AchievementEvaluator evaluator){
        this.rules=rules;this.records=records;this.player=player;this.playerService=playerService;this.players=players;
        this.levels=levels;this.events=events;this.stats=stats;this.catalog=catalog;this.copy=copy;this.evaluator=evaluator;
    }
    /** Migration hook: recognize facts that predate v0.4 without replaying rewards. */
    @PostConstruct public void migrateExistingFacts(){GrowthStats existing=stats.current();List<String> achievements=unlockAchievements(existing);List<String> titles=unlockTitles(achievements);if(!achievements.isEmpty()||!titles.isEmpty())players.save(player);}
    public synchronized GrowthResult process(LifeEvent event) {
        Optional<GrowthEventRecord> previous=records.find(event.id());
        if(previous.isPresent()){GrowthEventRecord r=previous.get();return result(r,true);}
        GrowthRules.Change change=rules.evaluate(event); int beforeLevel=levels.level(player.exp); int beforeEnergy=player.energy;
        int requestedDelta=change.energy();
        if(requestedDelta!=0) playerService.addEnergy(player,requestedDelta);
        int actualDelta=player.energy-beforeEnergy;
        int expDelta=0;
        if(change.type()==EnergyChangeType.SPEND&&actualDelta<0){
            GrowthEconomy.SpendConversion conversion=rules.convertSpentEnergy(player.exp_conversion_remainder,-actualDelta);
            expDelta=conversion.expGained();
            player.exp_conversion_remainder=conversion.newRemainder();
            if(expDelta>0) playerService.addExp(player,expDelta);
        }
        if(event.type()==LifeEventType.TASK_COMPLETED){
            if("special".equalsIgnoreCase(String.valueOf(event.metadata().get("taskSource")))) playerService.incrementSpecialTaskDone(player);
            else playerService.incrementTaskDone(player);
        }
        GrowthStats currentStats=stats.apply(event);int afterLevel=levels.level(player.exp); List<String> achievements=unlockAchievements(currentStats); List<String> titles=unlockTitles(achievements);
        players.save(player);
        if(actualDelta!=0) records.appendEnergy(new EnergyRecord(UUID.randomUUID().toString(),actualDelta,
                beforeEnergy,player.energy,change.reason(),event.id(),event.occurredAt(),change.type(),requestedDelta));
        GrowthEventRecord receipt=new GrowthEventRecord(event.id(),Instant.now(),expDelta,actualDelta,
                beforeLevel,afterLevel,change.reason(),achievements,titles);
        records.append(receipt);
        if(afterLevel>beforeLevel) events.recordDerived(LifeEventType.LEVEL_UP,"growth",copy.levelUpTitle(),
                copy.levelUpDescription(beforeLevel,afterLevel),List.of(copy.growthTag()),Map.of("oldLevel",beforeLevel,"newLevel",afterLevel,"sourceEventId",event.id()));
        for(String id:achievements){var a=catalog.achievement(id);events.recordDerived(LifeEventType.ACHIEVEMENT_UNLOCKED,
                "achievement",a.name(),a.description(),List.of(copy.growthTag(),copy.achievementTag()),Map.of("achievementId",id,"sourceEventId",event.id()));}
        for(String id:titles){var t=catalog.title(id);events.recordDerived(LifeEventType.TITLE_UNLOCKED,"title",t.name(),
                t.description(),List.of(copy.growthTag(),copy.titleTag()),Map.of("titleId",id,"sourceEventId",event.id()));}
        return result(receipt,false);
    }
    public synchronized void recordDerivedEvent(){stats.recordDerivedEvent();}
    public synchronized List<String> recalculate(){GrowthStats rebuilt=stats.rebuild();List<String> achievements=unlockAchievements(rebuilt);List<String> titles=unlockTitles(achievements);if(!achievements.isEmpty()||!titles.isEmpty())players.save(player);for(String id:achievements){var a=catalog.achievement(id);events.recordDerived(LifeEventType.ACHIEVEMENT_UNLOCKED,"achievement",a.name(),a.description(),List.of(copy.growthTag(),copy.achievementTag()),Map.of("achievementId",id,"sourceEventId","recalculate"));}for(String id:titles){var t=catalog.title(id);events.recordDerived(LifeEventType.TITLE_UNLOCKED,"title",t.name(),t.description(),List.of(copy.growthTag(),copy.titleTag()),Map.of("titleId",id,"sourceEventId","recalculate"));}return achievements;}
    private List<String> unlockAchievements(GrowthStats values){List<String> unlocked=new ArrayList<>();for(var rule:catalog.achievements()){if(!player.unlocked_achievements.contains(rule.id())&&metric(values,rule.condition().metric())>=rule.condition().target()){playerService.unlockAchievement(player,rule.id());unlocked.add(rule.id());}}return unlocked;}
    private List<String> unlockTitles(List<String> achievementIds){List<String> unlocked=new ArrayList<>();for(var definition:catalog.titles()){if(achievementIds.contains(definition.sourceId())&&!player.unlocked_titles.contains(definition.id())){playerService.unlockTitle(player,definition.id());unlocked.add(definition.id());}}if(player.equipped_title.isBlank()&&!unlocked.isEmpty())playerService.equipTitle(player,unlocked.getFirst());return unlocked;}
    private long metric(GrowthStats s,GrowthMetric m){return evaluator.metric(m,s,levels.level(player.exp),player.energy);}
    private GrowthResult result(GrowthEventRecord r,boolean duplicate){return new GrowthResult(r.eventId(),r.expDelta(),r.energyDelta(),r.levelBefore(),r.levelAfter(),r.levelAfter()>r.levelBefore(),r.unlockedAchievements(),r.unlockedTitles(),r.result(),duplicate);}
}
