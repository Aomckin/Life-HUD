package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.dto.DashboardSummary;
import io.github.aomckin.lifehud.dto.FocusSessionView;
import io.github.aomckin.lifehud.dto.agent.AgentContext;
import io.github.aomckin.lifehud.repository.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Shared, read-only semantic aggregation layer for people and agents. */
@Service
public final class AgentContextService {
    private static final String SCHEMA = "1";
    private final LifeDateService dates; private final Player player; private final LevelService levels;
    private final FocusService focus; private final DailyTaskManager daily; private final SpecialTaskManager special;
    private final DreamRepository dreams; private final GoalRepository goals; private final DreamMilestoneRepository dreamMilestones;
    private final RitualRepository rituals; private final RitualExecutionRepository ritualExecutions;
    private final SleepRecordRepository sleeps; private final MealRecordRepository meals;
    private final ExerciseRecordRepository exercises; private final CheckInRepository checkIns;
    private final LifeRecordRepository lifeRecords; private final JournalEntryRepository journals;
    private final MediaRepository media; private final LifeEventRepository events; private final TimelineService timeline;
    private final GrowthRecordRepository growthRecords; private final GrowthSnapshotService snapshots;
    private final GrowthTitleService titles;
    private final DashboardHeadlineService headlines;

    public AgentContextService(LifeDateService dates, Player player, LevelService levels, FocusService focus,
            DailyTaskManager daily, SpecialTaskManager special, DreamRepository dreams, GoalRepository goals,
            DreamMilestoneRepository dreamMilestones, RitualRepository rituals, RitualExecutionRepository ritualExecutions,
            SleepRecordRepository sleeps, MealRecordRepository meals, ExerciseRecordRepository exercises,
            CheckInRepository checkIns, LifeRecordRepository lifeRecords, JournalEntryRepository journals,
            MediaRepository media, LifeEventRepository events, TimelineService timeline,
            GrowthRecordRepository growthRecords, GrowthSnapshotService snapshots, GrowthTitleService titles,
            DashboardHeadlineService headlines) {
        this.dates=dates;this.player=player;this.levels=levels;this.focus=focus;this.daily=daily;this.special=special;
        this.dreams=dreams;this.goals=goals;this.dreamMilestones=dreamMilestones;this.rituals=rituals;
        this.ritualExecutions=ritualExecutions;this.sleeps=sleeps;this.meals=meals;this.exercises=exercises;
        this.checkIns=checkIns;this.lifeRecords=lifeRecords;this.journals=journals;this.media=media;
        this.events=events;this.timeline=timeline;this.growthRecords=growthRecords;this.snapshots=snapshots;this.titles=titles;this.headlines=headlines;
    }

    public AgentContext.Today today() {
        Instant now=dates.now(); LocalDate day=dates.today();
        return new AgentContext.Today(SCHEMA,now,day,status(),focus(day,7),tasks(),latestSleepFor(day),
                latestToday(meals.all(),MealRecord::time),dreams(),rituals(day),media(),timeline(day,10));
    }

    public AgentContext.Recent recent(int days) {
        int safe=range(days,1,30,"days"); LocalDate end=dates.today(),start=end.minusDays(safe-1L);
        List<TimelineItem> items=timeline.timeline(null,start.toString(),end.toString(),null,null,1,100);
        long minutes=focus.history(200).stream().filter(v->!date(v.startedAt()).isBefore(start)).mapToLong(FocusSessionView::effectiveMinutes).sum();
        int completed=(int)items.stream().filter(v->"TASK".equalsIgnoreCase(v.source())).count();
        return new AgentContext.Recent(SCHEMA,dates.now(),start,end,safe,items.size(),minutes,completed,items);
    }
    public AgentContext.StatusResponse statusResponse(){return new AgentContext.StatusResponse(SCHEMA,dates.now(),status());}
    public AgentContext.FocusResponse focusResponse(){return new AgentContext.FocusResponse(SCHEMA,dates.now(),dates.today(),focus(dates.today(),7));}
    public AgentContext.TasksResponse tasksResponse(){return new AgentContext.TasksResponse(SCHEMA,dates.now(),dates.today(),tasks());}
    public AgentContext.DreamsResponse dreamsResponse(){return new AgentContext.DreamsResponse(SCHEMA,dates.now(),dreams());}
    public AgentContext.LifeResponse lifeResponse(){LocalDate day=dates.today();return new AgentContext.LifeResponse(SCHEMA,dates.now(),day,life(day));}
    public AgentContext.JournalResponse journalResponse(int limit){int safe=range(limit,1,100,"limit");return new AgentContext.JournalResponse(SCHEMA,dates.now(),journals.all().stream().sorted(Comparator.comparing(JournalEntry::occurredAt).reversed()).limit(safe).toList(),timeline.timeline(null,null,null,null,null,1,safe));}
    public AgentContext.MediaResponse mediaResponse(){return new AgentContext.MediaResponse(SCHEMA,dates.now(),media());}
    public AgentContext.GrowthResponse growthResponse(){return new AgentContext.GrowthResponse(SCHEMA,dates.now(),growth());}
    public DashboardSummary dashboard(){AgentContext.Today v=today();return new DashboardSummary(v.generatedAt(),v.date(),headlines.random(),headlines.all(),v.status(),v.focus(),v.tasks(),life(v.date()),v.dreams(),v.rituals(),v.media(),v.timeline());}

    private AgentContext.Status status(){return new AgentContext.Status(player.energy,levels.level(player.exp),player.exp,
            titles.currentName(),latest(checkIns.all(),CheckIn::time),focus.current());}
    private AgentContext.Focus focus(LocalDate day,int historyDays){
        List<FocusSessionView> recent=focus.history(30); List<AgentContext.DailyFocus> dailyFocus=new ArrayList<>();
        for(int i=historyDays-1;i>=0;i--){LocalDate d=day.minusDays(i);long m=recent.stream().filter(v->date(v.startedAt()).equals(d)).mapToLong(FocusSessionView::effectiveMinutes).sum();dailyFocus.add(new AgentContext.DailyFocus(d,m));}
        List<FocusSessionView> today=recent.stream().filter(v->date(v.startedAt()).equals(day)).toList();
        return new AgentContext.Focus(today.stream().mapToLong(FocusSessionView::effectiveMinutes).sum(),today.size(),focus.current(),recent,List.copyOf(dailyFocus));
    }
    private AgentContext.Tasks tasks(){
        List<AgentContext.TaskItem> items=new ArrayList<>();
        daily.tasks().forEach(v->items.add(new AgentContext.TaskItem(v.id,v.name,v.done,false,v.dreamId,v.goalId,v.dreamMilestoneId)));
        special.tasks().forEach(v->items.add(new AgentContext.TaskItem(v.id,v.name,v.done,true,v.dreamId,v.goalId,v.dreamMilestoneId)));
        int completed=(int)items.stream().filter(AgentContext.TaskItem::completed).count();return new AgentContext.Tasks(completed,items.size()-completed,List.copyOf(items));
    }
    private AgentContext.Dreams dreams(){List<Dream> active=dreams.all().stream().filter(v->v.status()==DirectionStatus.ACTIVE).toList();Set<String> ids=new HashSet<>();active.forEach(v->ids.add(v.id()));List<Goal> gs=goals.all().stream().filter(v->ids.contains(v.dreamId())&&v.status()==DirectionStatus.ACTIVE).toList();Set<String> goalIds=new HashSet<>();gs.forEach(v->goalIds.add(v.id()));return new AgentContext.Dreams(active,gs,dreamMilestones.all().stream().filter(v->goalIds.contains(v.goalId())).toList());}
    private AgentContext.Rituals rituals(LocalDate day){return new AgentContext.Rituals(ritualExecutions.all().stream().filter(v->v.status()==RitualExecutionStatus.COMPLETED&&dates.isOn(v.completedAt(),day)).sorted(Comparator.comparing(RitualExecution::completedAt).reversed()).toList(),rituals.all().stream().filter(Ritual::enabled).toList());}
    private AgentContext.Life life(LocalDate day){return new AgentContext.Life(latestSleepFor(day),today(meals.all(),MealRecord::time),latest(exercises.all(),ExerciseRecord::startTime),latest(checkIns.all(),CheckIn::time),today(lifeRecords.all(),LifeRecord::time));}
    private AgentContext.Media media(){
        List<AnimeWatchSession> animeSessions=media.animeSessions().stream().sorted(Comparator.comparing(AnimeWatchSession::watchedAt).reversed()).limit(10).toList();
        List<MediaGameSession> gameSessions=media.gameSessions().stream().sorted(Comparator.comparing(MediaGameSession::endTime).reversed()).limit(10).toList();
        List<AgentContext.CompletedMedia> completed=Stream.concat(media.anime().stream().filter(v->v.status()==AnimeStatus.COMPLETED).map(v->new AgentContext.CompletedMedia(v.id(),"ANIME",v.title(),v.finishedAt())),media.games().stream().filter(v->v.status()==MediaGameStatus.COMPLETED).map(v->new AgentContext.CompletedMedia(v.id(),"GAME",v.title(),v.finishedAt()))).filter(v->v.finishedAt()!=null).sorted(Comparator.comparing(AgentContext.CompletedMedia::finishedAt).reversed()).limit(10).toList();
        return new AgentContext.Media(media.anime().stream().filter(v->v.status()==AnimeStatus.WATCHING).toList(),media.games().stream().filter(v->v.status()==MediaGameStatus.PLAYING).toList(),animeSessions,gameSessions,media.items().stream().filter(v->v.status()==MediaItemStatus.IN_PROGRESS).toList(),completed);
    }
    private AgentContext.Growth growth(){return new AgentContext.Growth(player.energy,player.exp,levels.level(player.exp),titles.currentName(),growthRecords.recent(20),snapshots.recent(7));}
    private SleepRecord latestSleepFor(LocalDate day){Instant boundary=dates.endExclusive(day);return sleeps.all().stream().filter(v->v.wakeTime()!=null&&v.wakeTime().isBefore(boundary)).max(Comparator.comparing(SleepRecord::wakeTime)).orElse(null);}
    private List<TimelineItem> timeline(LocalDate day,int limit){return timeline.timeline(day.toString(),null,null,null,null,1,limit);}
    private LocalDate date(Instant value){return LocalDate.ofInstant(value,dates.zone());}
    private <T> List<T> today(List<T> values,java.util.function.Function<T,Instant> time){return values.stream().filter(v->dates.isOn(time.apply(v),dates.today())).sorted(Comparator.comparing(time).reversed()).toList();}
    private <T> T latestToday(List<T> values,java.util.function.Function<T,Instant> time){return today(values,time).stream().findFirst().orElse(null);}
    private <T> T latest(List<T> values,java.util.function.Function<T,Instant> time){return values.stream().filter(v->time.apply(v)!=null).max(Comparator.comparing(time)).orElse(null);}
    private int range(int value,int min,int max,String name){if(value<min||value>max)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,name+" 必须在 "+min+" 到 "+max+" 之间");return value;}
}
