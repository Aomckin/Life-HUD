package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.dto.*;
import io.github.aomckin.lifehud.repository.FocusSessionRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Owns the Focus aggregate: session lifecycle, segments, elapsed time, and events. */
@Service public final class FocusService {
    private final FocusSessionRepository sessions; private final LifeEventService events; private final Clock clock;
    public FocusService(FocusSessionRepository sessions, LifeEventService events, Clock clock) { this.sessions=sessions; this.events=events; this.clock=clock; }

    public synchronized FocusSessionView start(FocusStartRequest r) {
        if(sessions.current().isPresent()) conflict("已有正在进行的 Focus");
        if(r==null||r.mode()==null) bad("请选择 Focus 模式");
        if(r.mode()==FocusMode.POMODORO&&(r.plannedMinutes()==null||r.plannedMinutes()<1||r.plannedMinutes()>720)) bad("番茄时长需为 1 到 720 分钟");
        int rest=r.breakMinutes()==null?5:r.breakMinutes(); if(rest<1||rest>180) bad("休息时长需为 1 到 180 分钟");
        Instant now=clock.instant(); String title=clean(r.title(),"未命名专注"), task=clean(r.taskId(),null); List<String> ids=ids(r.relatedTaskIds(),task);
        FocusSession s=new FocusSession(UUID.randomUUID().toString(),r.mode(),FocusStatus.RUNNING,title,task,now,now,null,
                r.mode()==FocusMode.POMODORO?r.plannedMinutes():null,0,null,now,now,ids,List.of(segment(FocusSegmentType.FOCUS,title,task,null,now,0)),rest);
        save(s); event(LifeEventType.FOCUS_STARTED,s,modeLabel(s.mode())+"开始"); return view(s,now);
    }
    public synchronized FocusSessionView pause(String id) {
        FocusSession s=require(id); if(s.status()!=FocusStatus.RUNNING) conflict("只有运行中的 Focus 可以暂停"); Instant now=clock.instant();
        FocusSession next=copy(s,FocusStatus.PAUSED,null,null,s.actualSeconds(now),s.note(),close(s.segments(),now),now);
        save(next); event(LifeEventType.FOCUS_PAUSED,next,"Focus 暂停"); return view(next,now);
    }
    public synchronized FocusSessionView resume(String id) {
        FocusSession s=require(id); if(s.status()!=FocusStatus.PAUSED) conflict("只有暂停中的 Focus 可以恢复"); Instant now=clock.instant();
        List<FocusSegment> list=new ArrayList<>(segments(s)); FocusSegment last=list.isEmpty()?null:list.getLast();
        list.add(segment(last==null?FocusSegmentType.FOCUS:last.type(),last==null?s.title():last.title(),last==null?s.taskId():last.relatedTaskId(),null,now,list.size()));
        FocusSession next=copy(s,FocusStatus.RUNNING,now,null,s.accumulatedSeconds(),s.note(),list,now);
        save(next); event(LifeEventType.FOCUS_RESUMED,next,"Focus 恢复"); return view(next,now);
    }
    public synchronized FocusSessionView switchSegment(String id, FocusSegmentRequest r) {
        FocusSession s=require(id); if(s.status()!=FocusStatus.RUNNING) conflict("暂停时不能切换过程"); if(r==null||r.type()==null) bad("请选择过程类型");
        Instant now=clock.instant(); List<FocusSegment> list=new ArrayList<>(close(s.segments(),now));
        String fallback=switch(r.type()){case FOCUS->s.title();case BREAK->"休息";case INTERRUPTION->"中断";};
        list.add(segment(r.type(),clean(r.title(),fallback),clean(r.relatedTaskId(),null),clean(r.note(),null),now,list.size()));
        FocusSession next=copy(s,s.status(),s.activeSince(),null,s.accumulatedSeconds(),s.note(),list,now);
        save(next); event(LifeEventType.FOCUS_SEGMENT_CHANGED,next,r.type()+" · "+list.getLast().title()); return view(next,now);
    }
    public synchronized FocusSessionView updateSegment(String id,String segmentId,FocusSegmentRequest r) {
        FocusSession s=sessions.findById(id).orElseThrow(()->missing("Focus 不存在")); if(r==null) bad("缺少修改内容");
        List<FocusSegment> list=new ArrayList<>(); boolean found=false; Instant now=clock.instant();
        for(FocusSegment x:segments(s)){if(!x.id().equals(segmentId)){list.add(x);continue;} found=true; list.add(new FocusSegment(x.id(),r.type()==null?x.type():r.type(),clean(r.title(),x.title()),x.startedAt(),x.endedAt(),clean(r.relatedTaskId(),x.relatedTaskId()),clean(r.note(),x.note()),x.order(),x.createdAt(),now));}
        if(!found) throw missing("过程记录不存在"); FocusSession next=copy(s,s.status(),s.activeSince(),s.endedAt(),s.accumulatedSeconds(),s.note(),list,now); save(next); return view(next,now);
    }
    public synchronized FocusSessionView complete(String id,String note){return finish(id,FocusStatus.COMPLETED,note);}
    public synchronized FocusSessionView interrupt(String id,String note){return finish(id,FocusStatus.INTERRUPTED,note);}
    public synchronized FocusSessionView manual(FocusManualRequest r){
        if(r==null||r.startedAt()==null||r.endedAt()==null||!r.endedAt().isAfter(r.startedAt())) bad("补录结束时间必须晚于开始时间");
        Instant now=clock.instant(); String title=clean(r.title(),"补录铁幕"); List<String> taskIds=ids(r.relatedTaskIds(),null); String task=taskIds.isEmpty()?null:taskIds.getFirst();
        FocusSegment seg=new FocusSegment(UUID.randomUUID().toString(),FocusSegmentType.FOCUS,title,r.startedAt(),r.endedAt(),task,null,0,now,now); long seconds=Duration.between(r.startedAt(),r.endedAt()).getSeconds();
        FocusSession s=new FocusSession(UUID.randomUUID().toString(),FocusMode.IRON_CURTAIN,FocusStatus.COMPLETED,title,task,r.startedAt(),null,r.endedAt(),null,seconds,clean(r.note(),null),now,now,taskIds,List.of(seg),5);
        save(s);boolean crossMidnight=!r.startedAt().atZone(clock.getZone()).toLocalDate().equals(r.endedAt().atZone(clock.getZone()).toLocalDate());events.record(LifeEventType.FOCUS_FINISHED,"focus",s.title(),"补录铁幕 · "+format(seconds),List.of("focus"),Map.of("focusSessionId",s.id(),"mode",s.mode().name(),"status",s.status().name(),"actualSeconds",seconds,"effectiveSeconds",seconds,"crossMidnight",crossMidnight));return view(s,now);
    }
    public FocusSessionView current(){Instant now=clock.instant();return sessions.current().map(s->view(s,now)).orElse(null);}
    public List<FocusSessionView> history(int limit){Instant now=clock.instant();return sessions.all().stream().sorted(Comparator.comparing(FocusSession::startedAt).reversed()).limit(Math.max(1,Math.min(limit,200))).map(s->view(s,now)).toList();}
    public synchronized void delete(String id){FocusSession session=sessions.findById(id).orElseThrow(()->missing("Focus 不存在"));if(session.status()==FocusStatus.RUNNING||session.status()==FocusStatus.PAUSED)conflict("进行中的 Focus 不能删除，请先完成或中断");sessions.delete(id);events.all().stream().filter(event->id.equals(String.valueOf(event.metadata().get("focusSessionId")))).map(LifeEvent::id).toList().forEach(events::delete);}
    /** Cross-midnight sessions belong to their start date in v0.3; no duration is duplicated. */
    public FocusTodayView today(){Instant now=clock.instant();ZoneId zone=clock.getZone();LocalDate date=LocalDate.ofInstant(now,zone);List<FocusSession> all=sessions.all().stream().filter(s->LocalDate.ofInstant(s.startedAt(),zone).equals(date)).toList();EnumMap<FocusMode,Long> modes=new EnumMap<>(FocusMode.class);for(FocusMode m:FocusMode.values())modes.put(m,0L);long effective=0,actual=0,longest=0;for(FocusSession s:all){long e=s.effectiveSeconds(now);effective+=e;actual+=s.actualSeconds(now);longest=Math.max(longest,e);modes.merge(s.mode(),e,Long::sum);}return new FocusTodayView(effective,effective/60,actual,all.size(),longest,Map.of("IRON_CURTAIN",modes.get(FocusMode.IRON_CURTAIN),"POMODORO",modes.get(FocusMode.POMODORO),"FREE",modes.get(FocusMode.FREE)));}

    private FocusSessionView finish(String id,FocusStatus status,String note){FocusSession s=require(id);Instant now=clock.instant();long actual=s.actualSeconds(now);FocusSession next=copy(s,status,null,now,actual,clean(note,null),close(s.segments(),now),now);save(next);boolean crossMidnight=!next.startedAt().atZone(clock.getZone()).toLocalDate().equals(now.atZone(clock.getZone()).toLocalDate());events.record(LifeEventType.FOCUS_FINISHED,"focus",next.title(),modeLabel(next.mode())+" · "+format(actual),List.of("focus"),Map.of("focusSessionId",next.id(),"mode",next.mode().name(),"status",status.name(),"actualSeconds",actual,"effectiveSeconds",next.effectiveSeconds(now),"crossMidnight",crossMidnight));return view(next,now);}
    private FocusSession require(String id){FocusSession s=sessions.findById(id).orElseThrow(()->missing("Focus 不存在"));FocusSession active=sessions.current().orElse(null);if(active==null||!active.id().equals(s.id()))conflict("这不是当前 Focus");return s;}
    private void save(FocusSession s){sessions.save(tasks(s));}
    private FocusSession tasks(FocusSession s){LinkedHashSet<String> ids=new LinkedHashSet<>(s.relatedTaskIds()==null?List.of():s.relatedTaskIds());if(s.taskId()!=null&&!s.taskId().isBlank())ids.add(s.taskId());segments(s).stream().map(FocusSegment::relatedTaskId).filter(Objects::nonNull).filter(v->!v.isBlank()).forEach(ids::add);return new FocusSession(s.id(),s.mode(),s.status(),s.title(),s.taskId(),s.startedAt(),s.activeSince(),s.endedAt(),s.plannedMinutes(),s.accumulatedSeconds(),s.note(),s.createdAt(),s.updatedAt(),List.copyOf(ids),segments(s),s.breakMinutes());}
    private FocusSession copy(FocusSession s,FocusStatus status,Instant active,Instant ended,long accumulated,String note,List<FocusSegment> list,Instant updated){return new FocusSession(s.id(),s.mode(),status,s.title(),s.taskId(),s.startedAt(),active,ended,s.plannedMinutes(),accumulated,note,s.createdAt(),updated,s.relatedTaskIds(),List.copyOf(list),s.breakMinutes());}
    private static List<FocusSegment> segments(FocusSession s){return s.segments()==null?List.of():s.segments();}
    private static List<FocusSegment> close(List<FocusSegment> source,Instant now){List<FocusSegment> out=new ArrayList<>();for(FocusSegment x:source==null?List.<FocusSegment>of():source)out.add(x.active()?new FocusSegment(x.id(),x.type(),x.title(),x.startedAt(),now,x.relatedTaskId(),x.note(),x.order(),x.createdAt(),now):x);return out;}
    private static FocusSegment segment(FocusSegmentType type,String title,String task,String note,Instant now,int order){return new FocusSegment(UUID.randomUUID().toString(),type,title,now,null,task,note,order,now,now);}
    private static List<String> ids(List<String> values,String first){LinkedHashSet<String> out=new LinkedHashSet<>();if(first!=null&&!first.isBlank())out.add(first.trim());if(values!=null)values.stream().filter(Objects::nonNull).map(String::trim).filter(v->!v.isBlank()).forEach(out::add);return List.copyOf(out);}
    private void event(LifeEventType type,FocusSession s,String content){events.record(type,"focus",s.title(),content,List.of("focus"),Map.of("focusSessionId",s.id(),"mode",s.mode().name()));}
    private FocusSessionView view(FocusSession s,Instant now){return FocusSessionView.from(s,now);}private static String clean(String v,String fallback){return v==null||v.isBlank()?fallback:v.trim();}
    private static String modeLabel(FocusMode m){return switch(m){case IRON_CURTAIN->"铁幕";case POMODORO->"番茄";case FREE->"自由专注";};}private static String format(long seconds){long h=seconds/3600,m=seconds%3600/60;return h>0?h+"h "+m+"min":m>0?m+"min":"<1min";}
    private static ResponseStatusException missing(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
