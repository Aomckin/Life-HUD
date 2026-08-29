package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.CheckInRequest;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.CheckInRepository;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** v0.6 check-in: 1~10 scales, quick facts, backfill dating. */
class CheckInServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w; CheckInService checkIns;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        checkIns = new CheckInService(new CheckInRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
    }

    @Test void scalesAreClampedTo1Through10(){
        var value = checkIns.create(new CheckInRequest(0, 99, 5, 10, null, ""));
        assertThat(value.energy()).isEqualTo(1);
        assertThat(value.mood()).isEqualTo(10);
        assertThat(value.focusDesire()).isEqualTo(5);
        assertThat(value.fatigue()).isEqualTo(10);
    }

    @Test void allFourScalesAreRequired(){
        assertThatThrownBy(() -> checkIns.create(new CheckInRequest(7, null, 5, 3, null, "")))
                .hasMessageContaining("打分");
    }

    @Test void backfillDatedWhenItHappened(){
        Instant yesterday = Instant.now().minusSeconds(24 * 3600);
        checkIns.create(new CheckInRequest(7, 8, 6, 3, yesterday, "昨天忘了记"));
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.CHECK_IN_RECORDED).findFirst().orElseThrow();
        assertThat(event.occurredAt()).isCloseTo(yesterday, within(1, java.time.temporal.ChronoUnit.MILLIS));
        assertThat(event.content()).contains("能量 7").contains("心情 8");
        assertThat(checkIns.latest().energy()).isEqualTo(7);
    }

    @Test void latestIsTheMostRecentByTimeNotByInsertion(){
        Instant now = Instant.now();
        checkIns.create(new CheckInRequest(3, 3, 3, 9, now, ""));
        checkIns.create(new CheckInRequest(8, 8, 8, 2, now.minusSeconds(600), "早一点但后记"));
        assertThat(checkIns.latest().energy()).isEqualTo(3);
    }
}
