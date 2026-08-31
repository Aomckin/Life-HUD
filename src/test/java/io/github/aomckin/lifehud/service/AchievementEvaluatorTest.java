package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class AchievementEvaluatorTest {
    @TempDir Path dir;
    @Test void evaluatesCrossDomainCountsFromLifeEvents() throws Exception {
        TestDataSupport data=new TestDataSupport(dir);LifeEventRepository repo=new LifeEventRepository(data.json,data.mapper);
        Instant now=Instant.now();repo.append(event("dream",LifeEventType.DREAM_CREATED,LifeEventSourceType.DREAM,now));repo.append(event("ritual",LifeEventType.RITUAL_COMPLETED,LifeEventSourceType.RITUAL,now));
        AchievementEvaluator evaluator=new AchievementEvaluator(repo,new MediaRepository(data.json,data.mapper),new LifeDateService());
        assertThat(evaluator.metric(GrowthMetric.DREAM_CREATED_COUNT,GrowthStats.empty(),1,100)).isEqualTo(1);
        assertThat(evaluator.metric(GrowthMetric.RITUAL_COMPLETED_COUNT,GrowthStats.empty(),1,100)).isEqualTo(1);
        assertThat(evaluator.metric(GrowthMetric.DAILY_DISTINCT_SOURCE_MAX,GrowthStats.empty(),1,100)).isEqualTo(2);
    }
    private LifeEvent event(String id,LifeEventType type,LifeEventSourceType source,Instant at){return new LifeEvent(id,type,source.name().toLowerCase(),id,id,at,at,List.of(),Map.of(),source,id,id,1);}
}
