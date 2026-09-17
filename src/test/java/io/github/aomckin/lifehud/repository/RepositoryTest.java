package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;import io.github.aomckin.lifehud.TestDataSupport;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class RepositoryTest {
 @TempDir Path temp;TestDataSupport data;@BeforeEach void setup()throws Exception{data=new TestDataSupport(temp);}
 @Test void jsonRoundTripPreservesUtf8AndResolvesConfiguredRoot(){ObjectNode n=data.mapper.createObjectNode().put("名称","Life HUD");data.json.write("utf8.json",n);assertThat(data.json.read("utf8.json").path("名称").asText()).isEqualTo("Life HUD");assertThat(data.json.resolve("utf8.json").getParent()).isEqualTo(temp.toAbsolutePath().normalize());}
 @Test void playerSavePreservesExactSchema(){var p=data.players.load(50,180);p.energy=77;p.action_counts.put("学习",3);p.exp_conversion_remainder=4;data.players.save(p);var n=data.json.read("save.json");assertThat(n.fieldNames()).toIterable().containsExactlyInAnyOrder("energy","exp","unlocked_achievements","action_counts","done_task_count","done_special_task_count","coin","shop_daily_purchases","shop_total_purchases","daily_double_exp_date","special_task_slots","unlocked_titles","equipped_title","completed_timed_actions","exp_conversion_remainder","energy_drift_date");assertThat(n.path("action_counts").path("学习").asInt()).isEqualTo(3);assertThat(n.path("exp_conversion_remainder").asInt()).isEqualTo(4);}
 @Test void missingSaveUsesConfiguredDefault()throws Exception{Files.delete(data.json.resolve("save.json"));var p=data.players.load(63,180);assertThat(p.energy).isEqualTo(63);assertThat(p.special_task_slots).isEqualTo(1);}
 @Test void logsKeepPythonShapes(){data.logs.action("执行行动：学习","能量+10",60);data.logs.timedAction("看番",60,-15,2,45);data.logs.abandon("学习",3);assertThat(data.logs.recent()).hasSize(3);assertThat(data.logs.recent().get(0)).contains("执行行动：学习","能量变化：能量+10","当前能量：60");assertThat(data.logs.recent().get(1)).contains("完成看番60分钟","经验 +2");assertThat(data.logs.recent().get(2)).contains("放弃学习","3分钟");}
 @Test void taskCatalogUpgradeAppendsMissingDefaultsWithoutOverwritingUserData()throws Exception{
  ObjectNode daily=data.mapper.createObjectNode();daily.put("last_update_date","2026-01-01");daily.putArray("active_task_ids");
  daily.putArray("tasks").add(data.mapper.createObjectNode().put("id","study_python").put("name","学习Python").put("reward",999).put("completed_count",42));
  data.json.write("tasks.json",daily);
  ObjectNode special=data.mapper.createObjectNode();special.put("last_update_date","2026-01-01");special.putArray("active_task_ids");
  special.putArray("tasks").add(data.mapper.createObjectNode().put("id","deep_casting").put("name","进入心流状态1小时").put("exp",20).put("completed_count",7));
  data.json.write("special_tasks.json",special);

  data.json.initialize();
  var upgradedDaily=data.json.read("tasks.json");var upgradedSpecial=data.json.read("special_tasks.json");
  assertThat(upgradedDaily.path("tasks").size()).isEqualTo(28);
  assertThat(upgradedDaily.path("tasks").get(0).path("reward").asInt()).isEqualTo(999);
  assertThat(upgradedDaily.path("tasks").get(0).path("completed_count").asInt()).isEqualTo(42);
  assertThat(upgradedDaily.path("tasks").get(0).has("note")).isTrue();
  assertThat(upgradedSpecial.path("tasks").size()).isEqualTo(59);
  assertThat(upgradedSpecial.path("tasks").get(0).path("exp").asInt()).isEqualTo(5);
  assertThat(upgradedSpecial.path("tasks").get(0).path("completed_count").asInt()).isEqualTo(7);
  assertThat(upgradedSpecial.path("reward_schema").asText()).isEqualTo("exp-v1");
  assertThat(java.util.stream.StreamSupport.stream(upgradedSpecial.path("tasks").spliterator(),false)
    .filter(v->"solve_algorithm_without_answer".equals(v.path("id").asText())).findFirst().orElseThrow().path("exp").asInt()).isEqualTo(3);

  data.json.initialize();
  assertThat(data.json.read("tasks.json").path("tasks").size()).isEqualTo(28);
  assertThat(data.json.read("special_tasks.json").path("tasks").size()).isEqualTo(59);
 }
}
