package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

/** v0.5.3.1 task pool semantics: definitions are the pool, the daily draw is today's instances. */
class TaskPoolTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);w=V05TestWiring.create(data); }

    @Test void addEditDisableRemoveDefinition(){
        var created=w.dailyTasks.addDefinition("写 v0.5.3 验收", "先写最小闭环", 6, 10);
        assertThat(w.dailyTasks.allTasks()).extracting("name").contains("写 v0.5.3 验收");
        var updated=w.dailyTasks.updateDefinition(created.id,"写 v0.5.3 验收（修订）","完成后回看",8,12);
        assertThat(updated.reward).isEqualTo(8);
        assertThat(updated.note).isEqualTo("完成后回看");
        assertThat(data.json.read("tasks.json").path("tasks").findValue("note").asText()).isNotNull();
        w.dailyTasks.setEnabled(created.id,false);
        assertThat(w.dailyTasks.allTasks().stream().filter(v->v.id.equals(created.id)).findFirst().orElseThrow().enabled).isFalse();
        w.dailyTasks.removeDefinition(created.id);
        assertThat(w.dailyTasks.allTasks()).noneMatch(v->v.id.equals(created.id));
    }

    @Test void disabledDefinitionsAreNeverDrawn(){
        w.specialTasks.setSlotCount(w.specialTasks.allTasks().size()+1); // include the whole enabled pool deterministically
        var created=w.specialTasks.addDefinition("隐藏挑战", 15);
        assertThat(w.specialTasks.tasks().stream().map(t->t.id)).contains(created.id);
        w.specialTasks.setEnabled(created.id,false);
        assertThat(w.specialTasks.tasks().stream().map(t->t.id)).doesNotContain(created.id);
        w.specialTasks.setEnabled(created.id,true);
        assertThat(w.specialTasks.tasks().stream().map(t->t.id)).contains(created.id);
    }

    @Test void disabledDailyDefinitionsAreNeverDrawn(){
        var created=w.dailyTasks.addDefinition("只在工作日出现的日常", 5, 5);
        w.dailyTasks.setEnabled(created.id,false);
        assertThat(w.dailyTasks.tasks().stream().map(t->t.id)).doesNotContain(created.id);
        w.dailyTasks.removeDefinition(created.id);
        assertThat(w.dailyTasks.allTasks()).noneMatch(v->v.id.equals(created.id));
    }

    @Test void specialDefinitionPersistsNote(){
        var created=w.specialTasks.addDefinition("完成一次挑战", "做完写两句复盘", 4);
        var updated=w.specialTasks.updateDefinition(created.id,"完成一次挑战","保留一张照片",5);
        assertThat(updated.note).isEqualTo("保留一张照片");
        assertThat(updated.exp).isEqualTo(5);
    }
}
