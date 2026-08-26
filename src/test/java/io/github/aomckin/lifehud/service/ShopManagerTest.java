package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import java.nio.file.Path;import java.time.LocalDate;import static org.assertj.core.api.Assertions.assertThat;
class ShopManagerTest {
 @TempDir Path temp;TestDataSupport data;@BeforeEach void setup()throws Exception{data=new TestDataSupport(temp);data.baseline(50,0,500);}
 private ShopManager manager(LocalDate date){var p=data.players.load(50,180);return new ShopManager(p,data.players,data.json,data.mapper,new PlayerService(),()->date);}
 @Test void dailyItemOnlyOncePerDay(){var m=manager(LocalDate.of(2026,8,25));assertThat(m.buy("daily_double_exp").get("success")).isEqualTo(true);assertThat(m.buy("daily_double_exp").get("message")).isEqualTo("商品已达购买上限");assertThat(manager(LocalDate.of(2026,8,26)).canBuy("daily_double_exp")).isTrue();}
 @Test void doubleExpMarksExactDate(){var m=manager(LocalDate.of(2026,8,25));m.buy("daily_double_exp");assertThat(data.save().path("daily_double_exp_date").asText()).isEqualTo("2026-08-25");assertThat(m.taskExpMultiplier()).isEqualTo(2);}
 @Test void permanentSlotOnlyAppliesOnce(){var m=manager(LocalDate.now());assertThat(m.buy("special_task_slot").get("success")).isEqualTo(true);assertThat(data.save().path("special_task_slots").asInt()).isEqualTo(2);assertThat(m.buy("special_task_slot").get("success")).isEqualTo(false);assertThat(data.save().path("special_task_slots").asInt()).isEqualTo(2);}
 @Test void insufficientCoinDoesNotMutate(){data.baseline(50,0,0);var m=manager(LocalDate.now());assertThat(m.buy("archmage_title").get("message")).isEqualTo("金币不足");assertThat(data.save().path("coin").asInt()).isZero();}
 @Test void titleItemUnlocksExactlyOnce(){var m=manager(LocalDate.now());assertThat(m.buy("archmage_title").get("success")).isEqualTo(true);assertThat(data.save().path("unlocked_titles")).extracting(n->n.asText()).contains("archmage");assertThat(m.buy("archmage_title").get("success")).isEqualTo(false);}
}
