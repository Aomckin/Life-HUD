package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.LifeRecordRequest;
import io.github.aomckin.lifehud.repository.LifeRecordRepository;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** v0.6 generic records: WATER/CAFFEINE/... share one chain; CUSTOM is the escape hatch. */
class LifeRecordServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w; LifeRecordService records;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        records = new LifeRecordService(new LifeRecordRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
    }

    @Test void simpleTypesShareOneChainWithDefaultUnits(){
        records.create(new LifeRecordRequest("WATER", 500.0, null, null, null, null));
        records.create(new LifeRecordRequest("CAFFEINE", 1.0, null, null, null, null));
        records.create(new LifeRecordRequest("SUNLIGHT", 25.0, null, null, "午后的阳台", null));
        assertThat(records.all()).hasSize(3);
        assertThat(records.all().get(0).unit()).isEqualTo("ml");
        assertThat(records.all().get(1).unit()).isEqualTo("杯");
        assertThat(records.all().get(2).unit()).isEqualTo("min");
        assertThat(w.lifeEvents.all()).filteredOn(v -> v.type() == LifeEventType.LIFE_RECORDED).hasSize(3);
    }

    @Test void customRequiresALabel(){
        assertThatThrownBy(() -> records.create(new LifeRecordRequest("CUSTOM", 1.0, null, null, null, null)))
                .hasMessageContaining("名称");
        var value = records.create(new LifeRecordRequest("CUSTOM", 1.0, "次", null, "遛了一圈", Map.of("label", "遛狗")));
        assertThat(value.metadata()).containsEntry("label", "遛狗");
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.LIFE_RECORDED).findFirst().orElseThrow();
        assertThat(event.title()).isEqualTo("遛狗");
        assertThat(event.content()).contains("遛狗 1次");
    }

    @Test void negativeValuesAreRejected(){
        assertThatThrownBy(() -> records.create(new LifeRecordRequest("WATER", -5.0, null, null, null, null)))
                .hasMessageContaining("不小于 0");
    }
}
