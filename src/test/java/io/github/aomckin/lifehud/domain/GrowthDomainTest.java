package io.github.aomckin.lifehud.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GrowthDomainTest {
    @Test void legacyLifeEventGetsTypedV04Fields() throws Exception {
        String json="""
                {"id":"old-1","type":"TASK_COMPLETED","source":"task","title":"旧任务","content":"完成",\
                 "occurredAt":"2026-08-29T00:00:00Z","createdAt":"2026-08-29T00:00:00Z",\
                 "tags":["task"],"metadata":{"taskId":"daily-1"}}
                """;
        LifeEvent event=new ObjectMapper().findAndRegisterModules().readValue(json,LifeEvent.class);
        assertThat(event.sourceType()).isEqualTo(LifeEventSourceType.TASK);
        assertThat(event.sourceId()).isEqualTo("daily-1");
        assertThat(event.description()).isEqualTo("完成");
        assertThat(event.version()).isEqualTo(1);
    }

    @Test void v04ConditionVocabularyIsComplete() {
        assertThat(GrowthConditionType.values()).containsExactly(GrowthConditionType.COUNT,
                GrowthConditionType.TOTAL_DURATION, GrowthConditionType.SINGLE_DURATION,
                GrowthConditionType.STREAK, GrowthConditionType.LEVEL,
                GrowthConditionType.VALUE_THRESHOLD, GrowthConditionType.CUSTOM);
        assertThat(new EnergyState(300,0,180).current()).isEqualTo(180);
    }
}
