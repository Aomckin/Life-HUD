package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class GrowthEngineTest {
    @TempDir Path temp; TestDataSupport data; Context context;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(50,0,0);context=create(); }

    private LifeEvent focusEvent(long effectiveSeconds){
        return context.events.record(LifeEventType.FOCUS_FINISHED,"focus","深度工作","完成",List.of("focus"),
                Map.of("effectiveSeconds",effectiveSeconds));
    }
    private LifeEvent spendEvent(int requested){
        return context.events.record(LifeEventType.ENERGY_SPENT,"manual","娱乐记录","请求消费 "+requested+" Energy",List.of("growth","energy"),
                Map.of("changeType",EnergyChangeType.SPEND.name(),"requestedEnergy",requested));
    }

    /** Case A: Focus earns Energy only, at 3 effective minutes per point. */
    @Test void focusEarnsEnergyWithoutExpAndIsIdempotent(){
        LifeEvent event=focusEvent(7200);
        GrowthResult first=context.engine.process(event),duplicate=context.engine.process(event);
        assertThat(first.expDelta()).isZero();assertThat(first.energyDelta()).isEqualTo(40);
        assertThat(duplicate.duplicate()).isTrue();assertThat(context.player.exp).isZero();assertThat(context.player.energy).isEqualTo(90);
        assertThat(context.records.all()).hasSize(1);
        assertThat(context.records.energy()).singleElement().satisfies(record->{
            assertThat(record.type()).isEqualTo(EnergyChangeType.EARN);assertThat(record.reason()).isEqualTo("有效 Focus 120 分钟");
        });
    }

    /** Case B: near-cap Focus clamps the actual gain and never converts surplus into EXP. */
    @Test void energyCapClampsEarningAndNeverConvertsSurplus(){
        context.player.energy=178;
        GrowthResult result=context.engine.process(focusEvent(3600));
        assertThat(result.energyDelta()).isEqualTo(2);assertThat(context.player.energy).isEqualTo(180);
        assertThat(context.player.exp).isZero();assertThat(context.player.exp_conversion_remainder).isZero();
        assertThat(context.records.energy()).last().satisfies(record->{
            assertThat(record.requestedDelta()).isEqualTo(20);assertThat(record.delta()).isEqualTo(2);
        });
    }

    /** Case C: small spends accumulate an integer remainder pool; 7 then 5 → +1 EXP, remainder 2. */
    @Test void smallSpendsAccumulateRemainderIntoExpAndAreIdempotent(){
        LifeEvent first=spendEvent(7);
        GrowthResult firstResult=context.engine.process(first);
        assertThat(firstResult.expDelta()).isZero();assertThat(context.player.exp_conversion_remainder).isEqualTo(7);
        assertThat(context.engine.process(first).duplicate()).isTrue();
        assertThat(context.player.exp_conversion_remainder).isEqualTo(7);assertThat(context.player.energy).isEqualTo(43);
        GrowthResult second=context.engine.process(spendEvent(5));
        assertThat(second.expDelta()).isEqualTo(1);assertThat(context.player.exp_conversion_remainder).isEqualTo(2);
        assertThat(context.player.exp).isEqualTo(1);assertThat(context.player.energy).isEqualTo(38);
    }

    /** Case D: overspending converts only the actually available Energy. */
    @Test void spendClampsToAvailableEnergyAndSettlesFromActualAmount(){
        context.player.energy=6;
        GrowthResult result=context.engine.process(spendEvent(20));
        assertThat(result.energyDelta()).isEqualTo(-6);assertThat(result.expDelta()).isZero();
        assertThat(context.player.energy).isZero();assertThat(context.player.exp).isZero();
        assertThat(context.player.exp_conversion_remainder).isEqualTo(6);
        assertThat(context.records.energy()).last().satisfies(record->{
            assertThat(record.requestedDelta()).isEqualTo(-20);assertThat(record.delta()).isEqualTo(-6);
        });
    }

    /** Case E/F: ADJUST and DECAY change Energy but never EXP or the conversion remainder. */
    @Test void adjustAndDecayNeverProduceExpOrTouchRemainder(){
        context.player.exp_conversion_remainder=3;
        LifeEvent decay=context.events.record(LifeEventType.ENERGY_CHANGED,"system","夜晚衰减","Energy 自然衰减 20",List.of("growth","energy"),
                Map.of("changeType",EnergyChangeType.DECAY.name(),"energyDelta",-20));
        LifeEvent adjust=context.events.record(LifeEventType.ENERGY_CHANGED,"system","手动修正","Energy 调整 -100",List.of("growth","energy"),
                Map.of("changeType",EnergyChangeType.ADJUST.name(),"energyDelta",-100));
        GrowthResult decayResult=context.engine.process(decay),adjustResult=context.engine.process(adjust);
        assertThat(adjustResult.expDelta()).isZero();assertThat(decayResult.expDelta()).isZero();
        assertThat(context.player.exp).isZero();assertThat(context.player.exp_conversion_remainder).isEqualTo(3);
        assertThat(context.records.energy()).extracting(EnergyRecord::type).containsExactly(EnergyChangeType.DECAY,EnergyChangeType.ADJUST);
    }

    /** Task: earns its baseEnergy, never EXP, and still drives achievements from real facts. */
    @Test void taskEarnsEnergyOnlyAndStillUnlocksAchievement(){
        LifeEvent event=context.events.record(LifeEventType.TASK_COMPLETED,"task","完成发布","完成",List.of("task"),
                Map.of("taskId","release","taskSource","daily","baseEnergy",6));
        GrowthResult result=context.engine.process(event);
        assertThat(result.expDelta()).isZero();assertThat(result.energyDelta()).isEqualTo(6);
        assertThat(context.player.exp).isZero();assertThat(context.player.energy).isEqualTo(56);
        assertThat(context.player.done_task_count).isEqualTo(1);assertThat(result.unlockedAchievements()).contains("task_first");
        assertThat(context.records.energy()).hasSize(1);
        assertThat(context.lifeEvents.all()).anyMatch(value->value.type()==LifeEventType.ACHIEVEMENT_UNLOCKED);
    }

    @Test void specialTaskGrantsDirectExpWithoutChangingEnergy(){
        LifeEvent event=context.events.record(LifeEventType.TASK_COMPLETED,"task","完成挑战","完成",List.of("task"),
                Map.of("taskId","challenge","taskSource","special","baseExp",7));
        GrowthResult result=context.engine.process(event);
        assertThat(result.expDelta()).isEqualTo(7);assertThat(result.energyDelta()).isZero();
        assertThat(context.player.exp).isEqualTo(7);assertThat(context.player.energy).isEqualTo(50);
        assertThat(context.records.energy()).isEmpty();
    }

    /** Case J: repeated real earn→spend cycles accumulate EXP and can cross a level. */
    @Test void repeatedRealSpendCyclesCanCrossMultipleLevels(){
        for(int i=0;i<2;i++) context.engine.process(focusEvent(14400));
        assertThat(context.player.energy).isEqualTo(180);
        context.engine.process(spendEvent(180));
        assertThat(context.player.exp).isEqualTo(18);assertThat(context.player.exp_conversion_remainder).isZero();
        for(int i=0;i<2;i++) context.engine.process(focusEvent(14400));
        context.engine.process(focusEvent(3600));
        assertThat(context.player.energy).isEqualTo(180);
        GrowthResult result=context.engine.process(spendEvent(180));
        assertThat(result.levelBefore()).isEqualTo(1);assertThat(result.levelAfter()).isEqualTo(2);assertThat(result.levelUp()).isTrue();
        assertThat(context.player.exp).isEqualTo(36);
        assertThat(context.lifeEvents.all()).anyMatch(value->value.type()==LifeEventType.LEVEL_UP);
    }

    /** v0.4.2 migration: a legacy exp_per_energy config converts once into energy_per_exp. */
    @Test void legacyExpPerEnergyConfigIsConvertedToEnergyPerExp(){
        var node=data.mapper.createObjectNode();node.put("exp_per_energy",0.5);data.json.write("growth.json",node);
        GrowthResult result=context.engine.process(spendEvent(10));
        assertThat(result.expDelta()).isEqualTo(5);
        assertThat(data.json.read("growth.json").has("exp_per_energy")).isFalse();
        assertThat(data.json.read("growth.json").path("energy_per_exp").asInt()).isEqualTo(2);
    }

    private Context create(){
        var player=data.players.load(50,180);var levels=new LevelService(data.json.read("level.json"));var playerService=new PlayerService();
        var lifeRepo=new LifeEventRepository(data.json,data.mapper);var events=new LifeEventService(lifeRepo);var records=new GrowthRecordRepository(data.json,data.mapper);
        var focus=new FocusSessionRepository(data.json,data.mapper);var milestones=new MilestoneRepository(data.json,data.mapper);
        var stats=new GrowthStatsService(new GrowthStatsRepository(data.json,data.mapper),focus,lifeRepo,milestones);
        var copy=new GrowthCopy(data.json);var catalog=new GrowthCatalog(data.json,data.mapper);
        var gameConfig=new io.github.aomckin.lifehud.core.GameConfig((com.fasterxml.jackson.databind.node.ObjectNode)data.json.read("config.json"));
        var engine=new GrowthEngine(new GrowthRules(new GrowthEconomy(data.json),copy,gameConfig),records,player,playerService,data.players,levels,events,stats,catalog,copy,new AchievementEvaluator(lifeRepo,new MediaRepository(data.json,data.mapper),new LifeDateService()));
        return new Context(player,events,engine,records,lifeRepo);
    }
    private record Context(Player player,LifeEventService events,GrowthEngine engine,GrowthRecordRepository records,LifeEventRepository lifeEvents) { }
}
