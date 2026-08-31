package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.LifeEventRepository;
import io.github.aomckin.lifehud.repository.MediaRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** Unified cross-domain achievement facts. Business services only emit facts. */
@Service
public final class AchievementEvaluator {
    private final LifeEventRepository events; private final MediaRepository media; private final LifeDateService dates;
    public AchievementEvaluator(LifeEventRepository events,MediaRepository media,LifeDateService dates){this.events=events;this.media=media;this.dates=dates;}

    public long metric(GrowthMetric metric,GrowthStats stats,int level,int energy){return switch(metric){
        case IRON_FOCUS_COUNT->stats.ironFocusCount();case POMODORO_FOCUS_COUNT->stats.pomodoroFocusCount();
        case FOCUS_COUNT->stats.focusCount();case EFFECTIVE_FOCUS_MINUTES->stats.effectiveFocusMinutes();
        case MAX_EFFECTIVE_FOCUS_MINUTES->stats.maxEffectiveFocusMinutes();case CROSS_MIDNIGHT_FOCUS_COUNT->stats.crossMidnightFocusCount();
        case TASK_COUNT->stats.taskCount();case LIFE_EVENT_COUNT->stats.lifeEventCount();case MILESTONE_COUNT->stats.milestoneCount();
        case LEVEL->level;case ENERGY->energy;
        case DREAM_CREATED_COUNT->count(LifeEventType.DREAM_CREATED);case DREAM_MILESTONE_COMPLETED_COUNT->count(LifeEventType.DREAM_MILESTONE_COMPLETED);
        case DREAM_COMPLETED_COUNT->count(LifeEventType.DREAM_COMPLETED);case RITUAL_COMPLETED_COUNT->count(LifeEventType.RITUAL_COMPLETED);
        case SLEEP_RECORDED_COUNT->count(LifeEventType.SLEEP_RECORDED);case MEAL_RECORDED_COUNT->count(LifeEventType.MEAL_RECORDED);
        case CHECK_IN_RECORDED_COUNT->count(LifeEventType.CHECK_IN_RECORDED);case EXERCISE_RECORDED_COUNT->count(LifeEventType.EXERCISE_RECORDED);
        case ANIME_SESSION_COUNT->count(LifeEventType.ANIME_WATCHED);case GAME_SESSION_COUNT->count(LifeEventType.GAME_PLAYED);
        case ANIME_COMPLETED_COUNT->media.anime().stream().filter(v->v.status()==AnimeStatus.COMPLETED).count();
        case GAME_COMPLETED_COUNT->media.games().stream().filter(v->v.status()==MediaGameStatus.COMPLETED).count();
        case LIFE_RECORD_DAY_STREAK->lifeDayStreak();case DAILY_DISTINCT_SOURCE_MAX->dailyDistinctSources();
    };}
    private long count(LifeEventType type){return events.all().stream().filter(v->v.type()==type).count();}
    private long lifeDayStreak(){Set<LocalDate> days=new HashSet<>();for(LifeEvent e:events.all())if(isLife(e.sourceType()))days.add(LocalDate.ofInstant(e.occurredAt(),dates.zone()));if(days.isEmpty())return 0;List<LocalDate> sorted=days.stream().sorted().toList();long best=1,current=1;for(int i=1;i<sorted.size();i++){current=sorted.get(i-1).plusDays(1).equals(sorted.get(i))?current+1:1;best=Math.max(best,current);}return best;}
    private long dailyDistinctSources(){Map<LocalDate,Set<LifeEventSourceType>> grouped=new HashMap<>();for(LifeEvent e:events.all())grouped.computeIfAbsent(LocalDate.ofInstant(e.occurredAt(),dates.zone()),ignored->new HashSet<>()).add(e.sourceType());return grouped.values().stream().mapToLong(Set::size).max().orElse(0);}
    private boolean isLife(LifeEventSourceType source){return source==LifeEventSourceType.SLEEP||source==LifeEventSourceType.MEAL||source==LifeEventSourceType.EXERCISE||source==LifeEventSourceType.CHECK_IN||source==LifeEventSourceType.LIFE_RECORD;}
}
