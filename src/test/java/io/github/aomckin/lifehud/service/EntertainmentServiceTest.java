package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.assertThat;

class EntertainmentServiceTest {
    @TempDir Path temp; TestDataSupport data; ServiceContext context;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);context=create(); }

    private EntertainmentRecord record(String title,int minutes,int cost){
        return context.entertainment.create(new EntertainmentRequest("GAME",title,minutes,cost,"",null));
    }

    /** Case A: a real entertainment record spends Energy through the unified ledger. */
    @Test void entertainmentSpendsEnergyThroughUnifiedLedger(){
        EntertainmentRecord value=record("舞萌",60,10);
        assertThat(value.category()).isEqualTo(EntertainmentCategory.GAME);
        assertThat(context.player.energy).isEqualTo(90);assertThat(context.player.exp).isEqualTo(1);
        assertThat(context.player.exp_conversion_remainder).isZero();
        assertThat(value.lifeEventId()).isNotBlank();
        assertThat(context.lifeEvents.all()).anyMatch(v->v.type()==LifeEventType.ENTERTAINMENT_RECORDED)
                .anyMatch(v->v.type()==LifeEventType.ENERGY_SPENT);
        assertThat(context.records.energy()).last().satisfies(r->{
            assertThat(r.type()).isEqualTo(EnergyChangeType.SPEND);assertThat(r.requestedDelta()).isEqualTo(-10);
            assertThat(r.delta()).isEqualTo(-10);assertThat(r.reason()).isEqualTo("舞萌");
        });
    }

    /** Case B: Energy shortfalls clamp the consumption; the record keeps the requested cost as fact. */
    @Test void insufficientEnergyRecordsRealityAndSettlesFromActualAmount(){
        data.baseline(5,0,0);context.player.energy=5;
        EntertainmentRecord value=record("深夜看番",90,20);
        assertThat(value.energyCost()).isEqualTo(20);
        assertThat(context.player.energy).isZero();assertThat(context.player.exp).isZero();
        assertThat(context.player.exp_conversion_remainder).isEqualTo(5);
        assertThat(context.records.energy()).last().satisfies(r->{
            assertThat(r.requestedDelta()).isEqualTo(-20);assertThat(r.delta()).isEqualTo(-5);
        });
    }

    /** Case C: consecutive small spends accumulate in the remainder pool: 4+3+5 → +1 EXP, remainder 2. */
    @Test void consecutiveSmallSpendsAccumulateIntoExp(){
        record("小游戏一",10,4);record("小游戏二",10,3);record("小游戏三",10,5);
        assertThat(context.player.exp).isEqualTo(1);assertThat(context.player.exp_conversion_remainder).isEqualTo(2);
        assertThat(context.player.energy).isEqualTo(88);
    }

    /** Case D: replaying the same spend event is idempotent — Energy and EXP move exactly once. */
    @Test void replayingSpendEventDoesNotSettleTwice(){
        record("舞萌",60,10);
        var spendEvent=context.lifeEvents.all().stream().filter(v->v.type()==LifeEventType.ENERGY_SPENT).findFirst().orElseThrow();
        GrowthResult replay=context.engine.process(spendEvent);
        assertThat(replay.duplicate()).isTrue();
        assertThat(context.player.energy).isEqualTo(90);assertThat(context.player.exp).isEqualTo(1);
        assertThat(context.player.exp_conversion_remainder).isZero();
    }

    /** Case H: a restart restores entertainment history, Energy, EXP and the conversion remainder. */
    @Test void restartRestoresRecordsEnergyExpAndRemainder() throws Exception{
        record("幼女战记",48,8);
        TestDataSupport restarted=new TestDataSupport(temp);
        Player player=restarted.players.load(0,180);
        assertThat(player.energy).isEqualTo(92);assertThat(player.exp).isZero();
        assertThat(player.exp_conversion_remainder).isEqualTo(8);
        EntertainmentRecord restored=new EntertainmentRecordRepository(restarted.json,restarted.mapper).all().getFirst();
        assertThat(restored.title()).isEqualTo("幼女战记");assertThat(restored.durationMinutes()).isEqualTo(48);
    }

    /** Updates keep the settled energyCost intact; deletion never rolls back the ledger. */
    @Test void updateKeepsCostAndDeleteKeepsLedger(){
        EntertainmentRecord value=record("舞萌",60,10);
        EntertainmentRecord updated=context.entertainment.update(value.id(),
                new EntertainmentRequest("ANIME","舞萌DX",90,null,"打得不错",null));
        assertThat(updated.energyCost()).isEqualTo(10);assertThat(updated.durationMinutes()).isEqualTo(90);
        assertThat(updated.category()).isEqualTo(EntertainmentCategory.ANIME);
        assertThat(context.player.energy).isEqualTo(90);
        context.entertainment.delete(value.id());
        assertThat(context.entertainment.all()).isEmpty();
        assertThat(context.player.energy).isEqualTo(90);assertThat(context.player.exp).isEqualTo(1);
        assertThat(context.player.exp_conversion_remainder).isZero();
        assertThat(context.lifeEvents.all()).anyMatch(v->v.type()==LifeEventType.ENTERTAINMENT_DELETED);
    }

    private ServiceContext create(){
        var player=data.players.load(100,180);
        var levels=new LevelService(data.json.read("level.json"));var playerService=new PlayerService();
        var lifeRepo=new LifeEventRepository(data.json,data.mapper);var records=new GrowthRecordRepository(data.json,data.mapper);
        var focus=new FocusSessionRepository(data.json,data.mapper);var milestones=new MilestoneRepository(data.json,data.mapper);
        var stats=new GrowthStatsService(new GrowthStatsRepository(data.json,data.mapper),focus,lifeRepo,milestones);
        var copy=new GrowthCopy(data.json);var catalog=new GrowthCatalog(data.json,data.mapper);
        var gameConfig=new GameConfig((ObjectNode)data.json.read("config.json"));
        final GrowthEngine[] holder=new GrowthEngine[1];
        var events=new LifeEventService(lifeRepo,new ObjectProvider<GrowthEngine>(){
            @Override public GrowthEngine getObject(){return holder[0];}
            @Override public GrowthEngine getObject(Object... args){return holder[0];}
            @Override public GrowthEngine getIfAvailable(){return holder[0];}
            @Override public GrowthEngine getIfUnique(){return holder[0];}
        });
        var engine=new GrowthEngine(new GrowthRules(new GrowthEconomy(data.json),copy,gameConfig),records,player,
                playerService,data.players,levels,events,stats,catalog,copy,
                new AchievementEvaluator(lifeRepo,new MediaRepository(data.json,data.mapper),new LifeDateService()));
        holder[0]=engine;
        var ledger=new EnergyLedgerService(events,records);
        var entertainment=new EntertainmentRecordService(new EntertainmentRecordRepository(data.json,data.mapper),events,ledger,copy);
        return new ServiceContext(player,engine,entertainment,records,lifeRepo);
    }
    private record ServiceContext(Player player,GrowthEngine engine,EntertainmentRecordService entertainment,
                                  GrowthRecordRepository records,LifeEventRepository lifeEvents) { }
}
