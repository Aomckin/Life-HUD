package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.assertThat;

class EnergyDriftServiceTest {
    @TempDir Path temp; TestDataSupport data; Context context;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);context=wiring(data); }

    /** Surplus regresses toward the midpoint: 100 → 90 + 10*0.75 = 98. */
    @Test void newDayMovesSurplusTowardMidpoint(){
        context.drift.applyIfNeeded();
        assertThat(context.player.energy).isEqualTo(98);
        assertThat(context.player.energy_drift_date).isEqualTo(LocalDate.now().toString());
        assertThat(context.records.energy()).last().satisfies(r->{
            assertThat(r.type()).isEqualTo(EnergyChangeType.ADJUST);assertThat(r.delta()).isEqualTo(-2);
            assertThat(r.reason()).contains("基准值 90");
        });
    }

    /** Deficit recovers upward: 0 → 90 - 90*0.75 = 23 (rounded). */
    @Test void newDayAlsoLiftsDeficitTowardMidpoint(){
        context.player.energy=0;
        context.drift.applyIfNeeded();
        assertThat(context.player.energy).isEqualTo(23);
        assertThat(context.records.energy()).last().satisfies(r->{
            assertThat(r.delta()).isEqualTo(23);assertThat(r.requestedDelta()).isEqualTo(23);
        });
    }

    /** Exactly at the midpoint nothing changes, and the day is still marked as processed. */
    @Test void midpointEnergyStaysUntouched(){
        context.player.energy=90;
        context.drift.applyIfNeeded();
        assertThat(context.player.energy).isEqualTo(90);
        assertThat(context.player.energy_drift_date).isEqualTo(LocalDate.now().toString());
        assertThat(context.records.energy()).isEmpty();
    }

    /** Same-day replays are no-ops; the drift never produces EXP. */
    @Test void appliesAtMostOncePerDayAndNeverYieldsExp(){
        context.drift.applyIfNeeded();
        int energy=context.player.energy;
        context.drift.applyIfNeeded();
        assertThat(context.player.energy).isEqualTo(energy);
        assertThat(context.player.exp).isZero();
        assertThat(context.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.ENERGY_CHANGED).hasSize(1);
    }

    /** A restart keeps the applied date; the next day drifts again from the drifted value (98 → 96). */
    @Test void driftDatePersistsAndNextDayDriftsFromNewValue() throws Exception{
        context.drift.applyIfNeeded();
        Context restarted=wiring(new TestDataSupport(temp));
        assertThat(restarted.player.energy).isEqualTo(98);
        assertThat(restarted.player.energy_drift_date).isEqualTo(LocalDate.now().toString());
        ObjectNode save=restarted.data.save();
        save.put("energy_drift_date",LocalDate.now().minusDays(1).toString());
        restarted.data.writeSave(save);
        Context nextDay=wiring(new TestDataSupport(temp));
        nextDay.drift.applyIfNeeded();
        assertThat(nextDay.player.energy).isEqualTo(96);
        assertThat(nextDay.player.exp).isZero();
    }

    private Context wiring(TestDataSupport support){
        var player=support.players.load(100,180);
        var levels=new LevelService(support.json.read("level.json"));var playerService=new PlayerService();
        var lifeRepo=new LifeEventRepository(support.json,support.mapper);var records=new GrowthRecordRepository(support.json,support.mapper);
        var focus=new FocusSessionRepository(support.json,support.mapper);var milestones=new MilestoneRepository(support.json,support.mapper);
        var stats=new GrowthStatsService(new GrowthStatsRepository(support.json,support.mapper),focus,lifeRepo,milestones);
        var copy=new GrowthCopy(support.json);var catalog=new GrowthCatalog(support.json,support.mapper);
        var gameConfig=new GameConfig((ObjectNode)support.json.read("config.json"));
        final GrowthEngine[] holder=new GrowthEngine[1];
        var events=new LifeEventService(lifeRepo,new ObjectProvider<GrowthEngine>(){
            @Override public GrowthEngine getObject(){return holder[0];}
            @Override public GrowthEngine getObject(Object... args){return holder[0];}
            @Override public GrowthEngine getIfAvailable(){return holder[0];}
            @Override public GrowthEngine getIfUnique(){return holder[0];}
        });
        var engine=new GrowthEngine(new GrowthRules(new GrowthEconomy(support.json),copy,gameConfig),records,player,
                playerService,support.players,levels,events,stats,catalog,copy);
        holder[0]=engine;
        var drift=new EnergyDriftService(player,support.players,events,copy,support.json);
        return new Context(support,player,drift,records,lifeRepo);
    }
    private record Context(TestDataSupport data,Player player,EnergyDriftService drift,
                           GrowthRecordRepository records,LifeEventRepository lifeEvents) { }
}
