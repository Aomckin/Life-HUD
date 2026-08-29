package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class RitualServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);w=V05TestWiring.create(data); }

    private Ritual morningRitual(){
        return w.rituals.create(new RitualRequest("晨光协议","让一天从光开始","生活",null,true,List.of(
                new RitualRequest.RitualStepInput(RitualStepType.TEXT,"拉开窗帘","让自然光进来",null,null,null,null),
                new RitualRequest.RitualStepInput(RitualStepType.MUSIC_HINT,"第一首歌","播放今天想听的第一首歌",null,null,null,null),
                new RitualRequest.RitualStepInput(RitualStepType.CHECK,"整理床铺","",null,null,null,null),
                new RitualRequest.RitualStepInput(RitualStepType.NOTE,"今天想做的事","",null,null,null,null)
        )));
    }

    @Test void createKeepsOrderedSteps(){
        Ritual ritual=morningRitual();
        List<RitualStep> steps=w.rituals.stepsOf(ritual.id());
        assertThat(steps).hasSize(4);
        assertThat(steps).extracting(RitualStep::title).containsExactly("拉开窗帘","第一首歌","整理床铺","今天想做的事");
        assertThat(steps).allMatch(RitualStep::required);
        assertThat(w.rituals.detail(ritual.id()).get("executions")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test void executionLifecycleEmitsExactlyOnce(){
        Ritual ritual=morningRitual();
        RitualExecution running=w.rituals.start(ritual.id());
        assertThat(running.status()).isEqualTo(RitualExecutionStatus.RUNNING);
        assertThat(running.stepResults()).hasSize(4);
        String stepId=running.stepResults().get(2).get("stepId").toString();
        w.rituals.updateStep(running.id(),stepId,true,"");
        w.rituals.updateStep(running.id(),running.stepResults().get(3).get("stepId").toString(),true,"写完 v0.5");
        RitualExecution done=w.rituals.complete(running.id(),"状态很好");
        RitualExecution again=w.rituals.complete(running.id(),"状态很好");
        assertThat(done.status()).isEqualTo(RitualExecutionStatus.COMPLETED);
        assertThat(again.id()).isEqualTo(done.id());
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.RITUAL_COMPLETED).hasSize(1);
        assertThat(w.player.exp).isZero();assertThat(w.player.energy).isEqualTo(100);
        assertThat(w.rituals.executionsOf(ritual.id())).hasSize(1);
    }

    @Test void cancelKeepsHistoryWithoutEvents(){
        Ritual ritual=morningRitual();
        RitualExecution running=w.rituals.start(ritual.id());
        RitualExecution cancelled=w.rituals.cancel(running.id(),"今天先算了");
        assertThat(cancelled.status()).isEqualTo(RitualExecutionStatus.CANCELLED);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.RITUAL_COMPLETED).isEmpty();
        assertThat(w.rituals.executionsOf(ritual.id())).hasSize(1);
    }

    @Test void disableHidesFromRunnableAndDeleteClearsSteps(){
        Ritual ritual=morningRitual();
        Ritual disabled=w.rituals.setEnabled(ritual.id(),false);
        assertThat(disabled.enabled()).isFalse();
        w.rituals.delete(ritual.id());
        assertThat(w.rituals.all()).isEmpty();assertThat(w.rituals.stepsOf(ritual.id())).isEmpty();
    }
}
