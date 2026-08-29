package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DreamServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);w=V05TestWiring.create(data); }

    private Dream dream(){
        return w.dreams.create(new DreamRequest("写完自己的小说","一部长篇","表达内心的世界",null,LocalDate.of(2027,1,1),"",""));
    }
    private Goal goal(String dreamId){
        return w.dreams.createGoal(dreamId,new GoalRequest("完成第一卷","",null,null,null));
    }
    private DreamMilestone milestone(String goalId){
        return w.dreams.createMilestone(goalId,new DreamMilestoneRequest("完成第一章","",null,null,null));
    }

    @Test void createEmitsFactsAndKeepsHierarchy(){
        Dream d=dream();Goal g=goal(d.id());DreamMilestone m=milestone(g.id());
        assertThat(w.dreams.all()).hasSize(1);assertThat(w.dreams.goalsOf(d.id())).hasSize(1);
        assertThat(w.dreams.milestonesOf(g.id())).hasSize(1);assertThat(m.status()).isEqualTo(DreamMilestoneStatus.PENDING);
        assertThat(w.lifeEvents.all()).anyMatch(v->v.type()==LifeEventType.DREAM_CREATED)
                .anyMatch(v->v.type()==LifeEventType.GOAL_CREATED);
        assertThat(w.player.exp).isZero();
    }

    @Test void completionsAreIdempotentAndEmitExactlyOnce(){
        Dream d=dream();Goal g=goal(d.id());DreamMilestone m=milestone(g.id());
        DreamMilestone first=w.dreams.completeMilestone(m.id()),second=w.dreams.completeMilestone(m.id());
        assertThat(first.completedAt()).isNotNull();assertThat(second.id()).isEqualTo(m.id());
        w.dreams.completeGoal(g.id());w.dreams.completeGoal(g.id());
        Dream done=w.dreams.complete(d.id());w.dreams.complete(d.id());
        assertThat(done.status()).isEqualTo(DirectionStatus.COMPLETED);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.DREAM_MILESTONE_COMPLETED).hasSize(1);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.GOAL_COMPLETED).hasSize(1);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.DREAM_COMPLETED).hasSize(1);
        assertThat(w.player.exp).isZero();assertThat(w.player.energy).isEqualTo(100);
    }

    @Test void linkInfersUpwardAndRejectsConflicts(){
        Dream d=dream();Goal g=goal(d.id());DreamMilestone m=milestone(g.id());
        String taskId=w.dailyTasks.allTasks().getFirst().id;
        DirectionInfo info=w.links.link("daily",taskId,"","",m.id());
        assertThat(info.dreamId()).isEqualTo(d.id());assertThat(info.goalId()).isEqualTo(g.id());
        var daily=w.data.json.read("tasks.json").path("tasks");
        assertThat(daily.get(0).path("dreamMilestoneId").asText()).isEqualTo(m.id());
        assertThatThrownBy(()->w.links.link("daily",taskId,"other-dream","",m.id())).hasMessageContaining("不一致");
        w.links.unlink("daily",taskId);
        assertThat(w.dailyTasks.allTasks().getFirst().dreamMilestoneId).isEmpty();
    }

    @Test void deletingGoalClearsTaskReferences(){
        Dream d=dream();Goal g=goal(d.id());DreamMilestone m=milestone(g.id());
        String taskId=w.specialTasks.allTasks().getFirst().id;
        w.links.link("special",taskId,d.id(),g.id(),m.id());
        w.dreams.deleteMilestone(m.id());
        assertThat(w.specialTasks.allTasks().getFirst().dreamMilestoneId).isEmpty();
        w.links.link("special",taskId,d.id(),g.id(),"");
        w.dreams.deleteGoal(g.id());
        assertThat(w.specialTasks.allTasks().getFirst().goalId).isEmpty();
        // The Dream itself still exists, so the task's dream link remains valid.
        assertThat(w.specialTasks.allTasks().getFirst().dreamId).isEqualTo(d.id());
        assertThat(w.dreams.goalsOf(d.id())).isEmpty();
    }

    @Test void updatePauseArchiveNeverEmitCompletionEvents(){
        Dream d=dream();
        w.dreams.update(d.id(),new DreamRequest("写完自己的小说（修订）",null,null,"PAUSED",null,null,null));
        assertThat(w.dreams.get(d.id()).status()).isEqualTo(DirectionStatus.PAUSED);
        Dream resumed=w.dreams.resume(d.id());assertThat(resumed.status()).isEqualTo(DirectionStatus.ACTIVE);
        Dream archived=w.dreams.archive(d.id());assertThat(archived.status()).isEqualTo(DirectionStatus.ARCHIVED);
        assertThat(w.dreams.all()).hasSize(1);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.DREAM_COMPLETED).isEmpty();
    }

    @Test void resolveHandlesDanglingIds(){
        Dream d=dream();Goal g=goal(d.id());DreamMilestone m=milestone(g.id());
        DirectionInfo info=w.dreams.resolve(d.id(),g.id(),m.id());
        assertThat(info.dreamTitle()).isEqualTo("写完自己的小说");assertThat(info.goalTitle()).isEqualTo("完成第一卷");
        DirectionInfo dangling=w.dreams.resolve("missing","","");
        assertThat(dangling.dreamTitle()).isEmpty();assertThat(dangling.empty()).isTrue();
        w.dreams.clearTaskReferences(Set.of(),Set.of(),Set.of());
    }
}
