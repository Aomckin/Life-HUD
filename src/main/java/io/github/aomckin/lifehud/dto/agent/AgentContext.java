package io.github.aomckin.lifehud.dto.agent;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.dto.FocusSessionView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Stable, strongly typed schema consumed by the future Chaoxi agent. */
public final class AgentContext {
    private AgentContext() { }

    public record Status(int energy, int level, int exp, String title, CheckIn checkIn,
                         FocusSessionView activeFocus) { }
    public record Focus(long effectiveMinutes, int sessionCount, FocusSessionView active,
                        List<FocusSessionView> recent, List<DailyFocus> daily) { }
    public record DailyFocus(LocalDate date, long effectiveMinutes) { }
    public record TaskItem(String id, String name, boolean completed, boolean special,
                           String dreamId, String goalId, String milestoneId) { }
    public record Tasks(int completed, int remaining, List<TaskItem> items) { }
    public record Dreams(List<Dream> active, List<Goal> goals, List<DreamMilestone> milestones) { }
    public record Rituals(List<RitualExecution> completedToday, List<Ritual> available) { }
    public record Life(SleepRecord sleep, List<MealRecord> meals, ExerciseRecord exercise,
                       CheckIn checkIn, List<LifeRecord> records) { }
    public record CompletedMedia(String id, String type, String title, Instant finishedAt) { }
    public record Media(List<Anime> watchingAnime, List<MediaGame> playingGames,
                        List<AnimeWatchSession> animeSessions, List<MediaGameSession> gameSessions,
                        List<MediaItem> items, List<CompletedMedia> recentlyCompleted) { }
    public record Growth(int energy, int exp, int level, String title,
                         List<GrowthEventRecord> recentEvents, List<GrowthSnapshot> snapshots) { }
    public record Today(String schemaVersion, Instant generatedAt, LocalDate date, Status status,
                        Focus focus, Tasks tasks, SleepRecord sleep, MealRecord meal,
                        Dreams dreams, Rituals rituals, Media media, List<TimelineItem> timeline) { }
    public record Recent(String schemaVersion, Instant generatedAt, LocalDate startDate, LocalDate endDate,
                         int days, int lifeEventCount, long focusMinutes, int tasksCompleted,
                         List<TimelineItem> timeline) { }
    public record StatusResponse(String schemaVersion, Instant generatedAt, Status status) { }
    public record FocusResponse(String schemaVersion, Instant generatedAt, LocalDate date, Focus focus) { }
    public record TasksResponse(String schemaVersion, Instant generatedAt, LocalDate date, Tasks tasks) { }
    public record DreamsResponse(String schemaVersion, Instant generatedAt, Dreams dreams) { }
    public record LifeResponse(String schemaVersion, Instant generatedAt, LocalDate date, Life life) { }
    public record JournalResponse(String schemaVersion, Instant generatedAt, List<JournalEntry> entries,
                                  List<TimelineItem> timeline) { }
    public record MediaResponse(String schemaVersion, Instant generatedAt, Media media) { }
    public record GrowthResponse(String schemaVersion, Instant generatedAt, Growth growth) { }
}
