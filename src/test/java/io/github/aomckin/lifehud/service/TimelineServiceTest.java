package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.ExerciseRequest;
import io.github.aomckin.lifehud.domain.JournalRequest;
import io.github.aomckin.lifehud.domain.MealRequest;
import io.github.aomckin.lifehud.domain.SleepRequest;
import io.github.aomckin.lifehud.domain.TimelineItem;
import io.github.aomckin.lifehud.repository.ExerciseRecordRepository;
import io.github.aomckin.lifehud.repository.JournalEntryRepository;
import io.github.aomckin.lifehud.repository.MealRecordRepository;
import io.github.aomckin.lifehud.repository.SleepRecordRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** v0.6 timeline: aggregation, occurredAt ordering, day filters, group filters, paging. */
class TimelineServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w; TimelineService timeline;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        var facts = new LifeFactRecorder(w.events);
        var copy = new GrowthCopy(data.json);
        var sleep = new SleepService(new SleepRecordRepository(data.json, data.mapper), facts, copy);
        var meals = new MealService(new MealRecordRepository(data.json, data.mapper), facts, copy);
        var journal = new JournalService(new JournalEntryRepository(data.json, data.mapper), facts, copy);
        var exercises = new ExerciseService(new ExerciseRecordRepository(data.json, data.mapper), facts, copy);
        // one day of life, recorded out of order
        journal.create(new JournalRequest("今天很喜欢傍晚的风。", Instant.parse("2026-08-29T15:48:00Z"), List.of(), List.of()));
        sleep.create(new SleepRequest(Instant.parse("2026-08-28T23:40:00Z"), Instant.parse("2026-08-29T07:11:00Z"), 4, "NIGHT", ""));
        meals.create(new MealRequest("LUNCH", Instant.parse("2026-08-29T04:17:00Z"), "两个肉包", null, "", List.of()));
        exercises.create(new ExerciseRequest("MAIMAI", Instant.parse("2026-08-29T12:13:00Z"), 92, "MEDIUM", ""));
        this.timeline = new TimelineService(w.events);
    }

    @Test void mixedSourcesAreSortedByOccurredAtNewestFirst(){
        List<TimelineItem> items = timeline.timeline("2026-08-29", null, null, null, null, 1, 50);
        assertThat(items).extracting(TimelineItem::type).containsExactly(
                "JOURNAL_WRITTEN", "EXERCISE_RECORDED", "SLEEP_RECORDED", "MEAL_RECORDED");
    }

    @Test void dateFilterUsesOccurredAtNotRecordedAt(){
        // the sleep fact was created "today" but happened across midnight from the 28th
        assertThat(timeline.timeline("2026-08-29", null, null, null, null, 1, 50)).hasSize(4);
        assertThat(timeline.timeline("2026-08-28", null, null, null, null, 1, 50)).isEmpty();
    }

    @Test void rangeFilterCoversBothDays(){
        assertThat(timeline.timeline(null, "2026-08-28", "2026-08-29", null, null, 1, 50)).hasSize(4);
        assertThat(timeline.timeline(null, "2026-08-01", "2026-08-28", null, null, 1, 50)).isEmpty();
    }

    @Test void sourceGroupsFilterCoarsely(){
        assertThat(timeline.timeline("2026-08-29", null, null, "life", null, 1, 50))
                .extracting(TimelineItem::type).containsExactly("EXERCISE_RECORDED", "SLEEP_RECORDED", "MEAL_RECORDED");
        assertThat(timeline.timeline("2026-08-29", null, null, "journal", null, 1, 50))
                .extracting(TimelineItem::type).containsExactly("JOURNAL_WRITTEN");
    }

    @Test void exactTypeFilterWorks(){
        assertThat(timeline.timeline("2026-08-29", null, null, null, "MEAL_RECORDED", 1, 50))
                .extracting(TimelineItem::type).containsExactly("MEAL_RECORDED");
    }

    @Test void pagingSlicesTheOrderedFeed(){
        assertThat(timeline.timeline("2026-08-29", null, null, null, null, 1, 2))
                .extracting(TimelineItem::type).containsExactly("JOURNAL_WRITTEN", "EXERCISE_RECORDED");
        assertThat(timeline.timeline("2026-08-29", null, null, null, null, 2, 2))
                .extracting(TimelineItem::type).containsExactly("SLEEP_RECORDED", "MEAL_RECORDED");
        assertThat(timeline.timeline("2026-08-29", null, null, null, null, 3, 2)).isEmpty();
    }

    @Test void invalidDatesAreRejected(){
        assertThatThrownBy(() -> timeline.timeline("08/29/2026", null, null, null, null, 1, 50))
                .hasMessageContaining("yyyy-MM-dd");
    }
}
