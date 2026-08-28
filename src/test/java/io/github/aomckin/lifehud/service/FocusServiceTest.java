package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.FocusMode;
import io.github.aomckin.lifehud.domain.FocusStatus;
import io.github.aomckin.lifehud.domain.FocusSegmentType;
import io.github.aomckin.lifehud.dto.FocusSegmentRequest;
import io.github.aomckin.lifehud.dto.FocusManualRequest;
import io.github.aomckin.lifehud.dto.FocusStartRequest;
import io.github.aomckin.lifehud.repository.FocusSessionRepository;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import io.github.aomckin.lifehud.repository.LifeEventRepository;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FocusServiceTest {
    @TempDir java.nio.file.Path data;
    private MutableClock clock;
    private FocusService focus;
    private JsonFileStore files;

    @BeforeEach void setUp() throws Exception {
        Files.createDirectories(data);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        files = new JsonFileStore(mapper, data);
        FocusSessionRepository sessions = new FocusSessionRepository(files, mapper);
        LifeEventService events = new LifeEventService(new LifeEventRepository(files, mapper));
        clock = new MutableClock(Instant.parse("2026-08-28T01:00:00Z"), ZoneOffset.UTC);
        focus = new FocusService(sessions, events, clock);
    }

    @Test void pauseTimeIsNotCountedAndSessionSurvivesRepositoryReads() {
        var started = focus.start(new FocusStartRequest(FocusMode.IRON_CURTAIN, "开发 v0.3", null, null));
        clock.advanceSeconds(600);
        var paused = focus.pause(started.id());
        assertThat(paused.actualSeconds()).isEqualTo(600);

        clock.advanceSeconds(300);
        assertThat(focus.current().actualSeconds()).isEqualTo(600);
        focus.resume(started.id());
        clock.advanceSeconds(600);

        var completed = focus.complete(started.id(), "后端完成");
        assertThat(completed.status()).isEqualTo(FocusStatus.COMPLETED);
        assertThat(completed.actualSeconds()).isEqualTo(1200);
        assertThat(completed.actualMinutes()).isEqualTo(20);
        assertThat(completed.note()).isEqualTo("后端完成");
        assertThat(focus.current()).isNull();
        assertThat(focus.today().totalSeconds()).isEqualTo(1200);
    }

    @Test void onlyOneCurrentSessionCanExist() {
        focus.start(new FocusStartRequest(FocusMode.FREE, "阅读", null, null));
        assertThatThrownBy(() -> focus.start(new FocusStartRequest(FocusMode.POMODORO, "写作", null, 25)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("已有正在进行");
    }

    @Test void pomodoroKeepsPlannedAndActualTimeSeparate() {
        var started = focus.start(new FocusStartRequest(FocusMode.POMODORO, "写测试", "task-1", 25));
        clock.advanceSeconds(35 * 60L);
        var completed = focus.complete(started.id(), null);
        assertThat(completed.plannedMinutes()).isEqualTo(25);
        assertThat(completed.actualMinutes()).isEqualTo(35);
        assertThat(completed.taskId()).isEqualTo("task-1");
    }

    @Test void crossingMidnightKeepsSessionWholeAndAssignsItToStartDate() {
        clock = new MutableClock(Instant.parse("2026-08-28T15:50:00Z"), ZoneId.of("Asia/Shanghai"));
        setClockOnService();
        var started = focus.start(new FocusStartRequest(FocusMode.IRON_CURTAIN, "跨日工作", null, null));

        clock.advanceSeconds(30 * 60L);
        assertThat(focus.current()).isNotNull();
        assertThat(focus.current().actualSeconds()).isEqualTo(1800);
        assertThat(focus.today().sessionCount()).isZero();
        assertThat(focus.today().totalSeconds()).isZero();

        var completed = focus.complete(started.id(), null);
        assertThat(completed.actualSeconds()).isEqualTo(1800);
        assertThat(completed.endedAt()).isAfter(completed.startedAt());
        assertThat(focus.today().sessionCount()).isZero();
    }

    @Test void rejectsInvalidPomodoroDurationsAndAcceptsBlankTitleSafely() {
        for (int minutes : new int[]{0, -1, 721, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> focus.start(new FocusStartRequest(FocusMode.POMODORO, "无效", null, minutes)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("1 到 720");
        }
        var started = focus.start(new FocusStartRequest(FocusMode.FREE, "   ", null, null));
        assertThat(started.title()).isEqualTo("未命名专注");
    }

    @Test void invalidStateTransitionsAndMissingSessionsAreRejected() {
        assertThatThrownBy(() -> focus.pause("missing"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Focus 不存在");
        var started = focus.start(new FocusStartRequest(FocusMode.FREE, "状态机", null, null));
        assertThatThrownBy(() -> focus.resume(started.id()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("只有暂停中");
        focus.pause(started.id());
        assertThatThrownBy(() -> focus.pause(started.id()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("只有运行中");
        focus.resume(started.id());
        focus.pause(started.id());
        focus.resume(started.id());
        focus.complete(started.id(), null);
        assertThatThrownBy(() -> focus.complete(started.id(), null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("不是当前 Focus");
    }

    @Test void segmentsSeparateFocusBreakAndInterruptionFromEffectiveTime() {
        var started = focus.start(new FocusStartRequest(FocusMode.IRON_CURTAIN, "算法", "task-a", null));
        clock.advanceSeconds(60 * 60L);
        focus.switchSegment(started.id(), new FocusSegmentRequest(FocusSegmentType.BREAK, "喝水", null, null));
        clock.advanceSeconds(20 * 60L);
        focus.switchSegment(started.id(), new FocusSegmentRequest(FocusSegmentType.FOCUS, "Life HUD", "task-b", null));
        clock.advanceSeconds(40 * 60L);
        var completed = focus.complete(started.id(), "完成两件事");

        assertThat(completed.actualSeconds()).isEqualTo(120 * 60L);
        assertThat(completed.effectiveSeconds()).isEqualTo(100 * 60L);
        assertThat(completed.segments()).extracting("type")
                .containsExactly(FocusSegmentType.FOCUS, FocusSegmentType.BREAK, FocusSegmentType.FOCUS);
        assertThat(completed.relatedTaskIds()).containsExactly("task-a", "task-b");
    }

    @Test void pauseClosesSegmentAndResumeCreatesContinuationWithoutCountingAwayTime() {
        var started = focus.start(new FocusStartRequest(FocusMode.IRON_CURTAIN, "项目", null, null));
        clock.advanceSeconds(60 * 60L);
        focus.pause(started.id());
        clock.advanceSeconds(30 * 60L);
        var resumed = focus.resume(started.id());
        clock.advanceSeconds(60 * 60L);
        var completed = focus.complete(resumed.id(), null);

        assertThat(completed.actualSeconds()).isEqualTo(120 * 60L);
        assertThat(completed.effectiveSeconds()).isEqualTo(120 * 60L);
        assertThat(completed.segments()).hasSize(2).allMatch(segment -> !segment.active());
    }

    @Test void manualIronCurtainCreatesOneEffectiveSegment() {
        Instant start = clock.instant().minusSeconds(90 * 60L);
        var recorded = focus.manual(new FocusManualRequest("补录学习", start, clock.instant(), "读完两章", List.of("study")));
        assertThat(recorded.status()).isEqualTo(FocusStatus.COMPLETED);
        assertThat(recorded.actualMinutes()).isEqualTo(90);
        assertThat(recorded.effectiveMinutes()).isEqualTo(90);
        assertThat(recorded.segments()).hasSize(1);
    }

    @Test void legacySessionWithoutSegmentsStillLoadsAndKeepsItsEffectiveTime() {
        Instant start = clock.instant().minusSeconds(600);
        files.write("focus-sessions.json", List.of(Map.ofEntries(
                Map.entry("id", "legacy"), Map.entry("mode", "FREE"), Map.entry("status", "COMPLETED"),
                Map.entry("title", "旧记录"), Map.entry("startedAt", start), Map.entry("endedAt", clock.instant()),
                Map.entry("accumulatedSeconds", 600), Map.entry("createdAt", start), Map.entry("updatedAt", clock.instant()))));

        var legacy = focus.history(10).getFirst();
        assertThat(legacy.id()).isEqualTo("legacy");
        assertThat(legacy.segments()).isEmpty();
        assertThat(legacy.effectiveSeconds()).isEqualTo(600);
    }

    private void setClockOnService() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JsonFileStore files = new JsonFileStore(mapper, data);
        focus = new FocusService(new FocusSessionRepository(files, mapper),
                new LifeEventService(new LifeEventRepository(files, mapper)), clock);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;
        private MutableClock(Instant instant, ZoneId zone) { this.instant = instant; this.zone = zone; }
        void advanceSeconds(long seconds) { instant = instant.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(instant, zone); }
        @Override public Instant instant() { return instant; }
    }
}
