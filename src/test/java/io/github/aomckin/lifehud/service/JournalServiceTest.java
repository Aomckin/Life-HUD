package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.JournalEntry;
import io.github.aomckin.lifehud.domain.JournalRequest;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.repository.JournalEntryRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** v0.6 manual journal: write, edit, delete — the timeline follows the record. */
class JournalServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w; JournalService journal;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        journal = new JournalService(new JournalEntryRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
    }

    private JournalEntry write() {
        return journal.create(new JournalRequest("今天很喜欢傍晚的风。", Instant.parse("2026-08-29T23:40:00Z"),
                List.of("/uploads/a.jpg"), List.of("生活", "傍晚")));
    }

    @Test void writeEmitsDatedFactWithTagsAndImages(){
        JournalEntry entry = write();
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.JOURNAL_WRITTEN).findFirst().orElseThrow();
        assertThat(event.sourceId()).isEqualTo(entry.id());
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-29T23:40:00Z"));
        assertThat(event.tags()).contains("journal");
        assertThat(event.metadata()).containsEntry("images", List.of("/uploads/a.jpg"));
        assertThat(event.metadata()).containsEntry("entryTags", List.of("生活", "傍晚"));
    }

    @Test void blankContentIsRejected(){
        assertThatThrownBy(() -> journal.create(new JournalRequest("   ", null, null, null)))
                .hasMessageContaining("内容");
    }

    @Test void editRewritesTheSameFact(){
        JournalEntry entry = write();
        String eventId = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.JOURNAL_WRITTEN).findFirst().orElseThrow().id();
        journal.update(entry.id(), new JournalRequest("改口：其实更喜欢清晨。", null, null, List.of("清晨")));
        var events = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.JOURNAL_WRITTEN).toList();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).id()).isEqualTo(eventId);
        assertThat(events.get(0).content()).contains("清晨");
        assertThat(events.get(0).version()).isEqualTo(2);
    }

    @Test void deleteRemovesTheFact(){
        JournalEntry entry = write();
        journal.remove(entry.id());
        assertThat(journal.all()).isEmpty();
        assertThat(w.lifeEvents.all()).noneMatch(v -> v.type() == LifeEventType.JOURNAL_WRITTEN);
    }
}
