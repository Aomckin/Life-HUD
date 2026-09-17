package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.LifeEventType;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

/** Action-desk completion by task id: emits TASK_COMPLETED exactly once. */
class TaskCompletionByIdTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);w=V05TestWiring.create(data); }

    @Test void completeByIdEmitsEventExactlyOnce(){
        String taskId=w.dailyTasks.allTasks().getFirst().id;
        var first=w.taskService.completeDailyById(taskId);
        assertThat(first.success()).isTrue();
        var second=w.taskService.completeDailyById(taskId);
        assertThat(second.success()).isTrue();
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.TASK_COMPLETED).hasSize(1);
        assertThat(w.player.done_task_count).isEqualTo(1);
        assertThat(w.dailyTasks.allTasks().getFirst().done).isTrue();
    }

    @Test void specialCompleteByIdEmitsEventExactlyOnce(){
        var task=w.specialTasks.allTasks().getFirst();
        String taskId=task.id;
        int beforeEnergy=w.player.energy,beforeExp=w.player.exp;
        w.taskService.completeSpecialById(taskId);
        w.taskService.completeSpecialById(taskId);
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.TASK_COMPLETED).hasSize(1);
        assertThat(w.player.done_special_task_count).isEqualTo(1);
        assertThat(w.player.energy).isEqualTo(beforeEnergy);
        assertThat(w.player.exp).isEqualTo(beforeExp+task.exp);
    }

    @Test void unknownTaskIsRejected(){
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> w.taskService.completeDailyById("missing"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> w.taskService.completeSpecialById("missing"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
