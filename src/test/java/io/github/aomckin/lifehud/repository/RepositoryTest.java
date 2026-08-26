package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;import io.github.aomckin.lifehud.TestDataSupport;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class RepositoryTest {
 @TempDir Path temp;TestDataSupport data;@BeforeEach void setup()throws Exception{data=new TestDataSupport(temp);}
 @Test void jsonRoundTripPreservesUtf8AndResolvesConfiguredRoot(){ObjectNode n=data.mapper.createObjectNode().put("名称","Life HUD");data.json.write("utf8.json",n);assertThat(data.json.read("utf8.json").path("名称").asText()).isEqualTo("Life HUD");assertThat(data.json.resolve("utf8.json").getParent()).isEqualTo(temp.toAbsolutePath().normalize());}
 @Test void playerSavePreservesExactSchema(){var p=data.players.load(50,180);p.energy=77;p.action_counts.put("学习",3);data.players.save(p);var n=data.json.read("save.json");assertThat(n.fieldNames()).toIterable().containsExactlyInAnyOrder("energy","exp","unlocked_achievements","action_counts","done_task_count","done_special_task_count","coin","shop_daily_purchases","shop_total_purchases","daily_double_exp_date","special_task_slots","unlocked_titles","equipped_title","completed_timed_actions");assertThat(n.path("action_counts").path("学习").asInt()).isEqualTo(3);}
 @Test void missingSaveUsesConfiguredDefault()throws Exception{Files.delete(data.json.resolve("save.json"));var p=data.players.load(63,180);assertThat(p.energy).isEqualTo(63);assertThat(p.special_task_slots).isEqualTo(1);}
 @Test void logsKeepPythonShapes(){data.logs.action("执行行动：学习","能量+10",60);data.logs.timedAction("看番",60,-15,2,45);data.logs.abandon("学习",3);assertThat(data.logs.recent()).hasSize(3);assertThat(data.logs.recent().get(0)).contains("执行行动：学习","能量变化：能量+10","当前能量：60");assertThat(data.logs.recent().get(1)).contains("完成看番60分钟","经验 +2");assertThat(data.logs.recent().get(2)).contains("放弃学习","3分钟");}
}
