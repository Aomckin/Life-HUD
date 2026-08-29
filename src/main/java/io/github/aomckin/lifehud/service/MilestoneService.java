package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.MilestoneRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public final class MilestoneService {
    private final MilestoneRepository values;private final LifeEventService events;
    public MilestoneService(MilestoneRepository values,LifeEventService events){this.values=values;this.events=events;}
    public List<Milestone> all(){return values.all();}
    public synchronized Milestone create(MilestoneRequest request){validate(request);Instant now=Instant.now();Milestone value=new Milestone(UUID.randomUUID().toString(),request.title().trim(),clean(request.description()),request.occurredAt()==null?LocalDate.now():request.occurredAt(),defaulted(request.category(),"生活"),MilestoneSource.MANUAL,request.relatedEventIds(),clean(request.relatedModule()),clean(request.media()),Boolean.TRUE.equals(request.pinned()),now,now);values.save(value);events.record(LifeEventType.MILESTONE_CREATED,"milestone",value.title(),value.description(),List.of("growth","milestone"),Map.of("milestoneId",value.id(),"occurredOn",value.occurredAt().toString()));return value;}
    public synchronized Milestone update(String id,MilestoneRequest request){validate(request);Milestone old=values.find(id).orElseThrow(()->missing());Milestone value=new Milestone(old.id(),request.title().trim(),clean(request.description()),request.occurredAt()==null?old.occurredAt():request.occurredAt(),defaulted(request.category(),old.category()),old.source(),request.relatedEventIds()==null?old.relatedEventIds():request.relatedEventIds(),defaulted(request.relatedModule(),old.relatedModule()),request.media()==null?old.media():request.media(),request.pinned()==null?old.pinned():request.pinned(),old.createdAt(),Instant.now());values.save(value);events.record(LifeEventType.MILESTONE_UPDATED,"milestone",value.title(),"里程碑已更新",List.of("growth","milestone"),Map.of("milestoneId",id));return value;}
    public synchronized void delete(String id){Milestone old=values.find(id).orElseThrow(()->missing());values.delete(id);events.record(LifeEventType.MILESTONE_DELETED,"milestone",old.title(),"里程碑已删除，原始事件仍保留。",List.of("growth","milestone"),Map.of("milestoneId",id));}
    private void validate(MilestoneRequest r){if(r==null||r.title()==null||r.title().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"里程碑标题不能为空");}
    private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"里程碑不存在");}private String clean(String v){return v==null?"":v.trim();}private String defaulted(String v,String d){String c=clean(v);return c.isBlank()?d:c;}
}
