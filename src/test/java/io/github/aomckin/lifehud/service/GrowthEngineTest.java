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

    @Test void focusGrowthUsesEffectiveMinutesAndIsIdempotent(){
        LifeEvent event=context.events.record(LifeEventType.FOCUS_FINISHED,"focus","深度工作","完成",List.of("focus"),Map.of("effectiveSeconds",3600));
        GrowthResult first=context.engine.process(event),duplicate=context.engine.process(event);
        assertThat(first.expDelta()).isEqualTo(12);assertThat(first.energyDelta()).isEqualTo(2);
        assertThat(duplicate.duplicate()).isTrue();assertThat(context.player.exp).isEqualTo(12);assertThat(context.player.energy).isEqualTo(52);
        assertThat(context.records.all()).hasSize(1);assertThat(context.records.energy()).singleElement().extracting(EnergyRecord::reason).isEqualTo("有效 Focus 60 分钟");
    }

    @Test void oneEventCanCrossMultipleLevelsAndUnlockTaskAchievement(){
        LifeEvent event=context.events.record(LifeEventType.TASK_COMPLETED,"task","完成发布","完成",List.of("task"),Map.of("taskId","release","taskSource","daily","baseExp",50,"baseEnergy",0));
        GrowthResult result=context.engine.process(event);
        assertThat(result.levelBefore()).isEqualTo(1);assertThat(result.levelAfter()).isEqualTo(3);
        assertThat(result.unlockedAchievements()).contains("task_first");assertThat(context.player.done_task_count).isEqualTo(1);
        assertThat(context.lifeEvents.all()).anyMatch(value->value.type()==LifeEventType.LEVEL_UP).anyMatch(value->value.type()==LifeEventType.ACHIEVEMENT_UNLOCKED);
    }

    private Context create(){
        var player=data.players.load(50,180);var levels=new LevelService(data.json.read("level.json"));var playerService=new PlayerService();
        var lifeRepo=new LifeEventRepository(data.json,data.mapper);var events=new LifeEventService(lifeRepo);var records=new GrowthRecordRepository(data.json,data.mapper);
        var focus=new FocusSessionRepository(data.json,data.mapper);var milestones=new MilestoneRepository(data.json,data.mapper);
        var stats=new GrowthStatsService(new GrowthStatsRepository(data.json,data.mapper),focus,lifeRepo,milestones);
        var engine=new GrowthEngine(new GrowthRules(),records,player,playerService,data.players,levels,events,stats);
        return new Context(player,events,engine,records,lifeRepo);
    }
    private record Context(Player player,LifeEventService events,GrowthEngine engine,GrowthRecordRepository records,LifeEventRepository lifeEvents) { }
}
