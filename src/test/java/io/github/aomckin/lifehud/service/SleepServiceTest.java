package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.SleepRecord;
import io.github.aomckin.lifehud.domain.SleepRequest;
import io.github.aomckin.lifehud.domain.SleepType;
import io.github.aomckin.lifehud.repository.SleepRecordRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** v0.6 sleep facts: cross-midnight math, backfill semantics and record↔event sync. */
class SleepServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w; SleepService sleep;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        sleep = new SleepService(new SleepRecordRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
    }

    private SleepRecord backfill(String from, String to) {
        return sleep.create(new SleepRequest(Instant.parse(from), Instant.parse(to), 4, "NIGHT", "补录"));
    }

    @Test void crossMidnightDurationIsComputedFromTimestamps(){
        SleepRecord record = backfill("2026-08-28T23:40:00Z", "2026-08-29T07:30:00Z");
        assertThat(record.durationMinutes()).isEqualTo(470);
        assertThat(record.type()).isEqualTo(SleepType.NIGHT);
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.SLEEP_RECORDED).findFirst().orElseThrow();
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-29T07:30:00Z"));
        assertThat(event.sourceId()).isEqualTo(record.id());
        assertThat(event.content()).contains("7h 50min").contains("4/5");
    }

    @Test void napAndSameDaySleepComputeCorrectly(){
        SleepRecord nap = backfill("2026-08-29T13:00:00Z", "2026-08-29T13:30:00Z");
        assertThat(nap.durationMinutes()).isEqualTo(30);
    }

    @Test void illegalSleepWindowsAreRejected(){
        assertThatThrownBy(() -> backfill("2026-08-29T09:00:00Z", "2026-08-29T09:00:00Z"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> backfill("2026-08-28T09:00:00Z", "2026-08-29T09:00:00Z"))
                .hasMessageContaining("20 小时");
    }

    @Test void updateRewritesTheSameFactInPlace(){
        SleepRecord record = backfill("2026-08-28T23:40:00Z", "2026-08-29T06:30:00Z");
        String eventId = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.SLEEP_RECORDED)
                .findFirst().orElseThrow().id();
        sleep.update(record.id(), new SleepRequest(null, Instant.parse("2026-08-29T08:10:00Z"), 5, null, null));
        var events = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.SLEEP_RECORDED).toList();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).id()).isEqualTo(eventId);
        assertThat(events.get(0).content()).contains("8h 30min").contains("5/5");
        assertThat(events.get(0).occurredAt()).isEqualTo(Instant.parse("2026-08-29T08:10:00Z"));
        assertThat(events.get(0).version()).isEqualTo(2);
    }

    @Test void deleteRemovesTheFactSoNoGhostRemains(){
        SleepRecord record = backfill("2026-08-28T23:40:00Z", "2026-08-29T07:00:00Z");
        sleep.remove(record.id());
        assertThat(sleep.all()).isEmpty();
        assertThat(w.lifeEvents.all()).noneMatch(v -> v.type() == LifeEventType.SLEEP_RECORDED);
    }

    @Test void imagesSurvivePartialUpdatesAndReachTheTimeline(){
        SleepRecord record = sleep.create(new SleepRequest(Instant.parse("2026-08-28T23:40:00Z"),
                Instant.parse("2026-08-29T07:00:00Z"), 4, "NIGHT", "", List.of("/uploads/sleep.jpg")));
        sleep.update(record.id(), new SleepRequest(null, null, 5, null, null));
        assertThat(sleep.get(record.id()).images()).containsExactly("/uploads/sleep.jpg");
        assertThat(w.lifeEvents.all().stream().filter(v->v.type()==LifeEventType.SLEEP_RECORDED).findFirst().orElseThrow().metadata()).containsEntry("images", List.of("/uploads/sleep.jpg"));
    }
}
