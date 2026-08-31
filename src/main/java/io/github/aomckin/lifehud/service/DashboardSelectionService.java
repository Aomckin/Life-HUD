package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.DashboardSelections;
import io.github.aomckin.lifehud.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public final class DashboardSelectionService {
    private final DashboardSelectionRepository values;private final DreamRepository dreams;private final RitualRepository rituals;private final MediaRepository media;
    public DashboardSelectionService(DashboardSelectionRepository values,DreamRepository dreams,RitualRepository rituals,MediaRepository media){this.values=values;this.dreams=dreams;this.rituals=rituals;this.media=media;}
    public DashboardSelections get(){return values.get();}
    public DashboardSelections select(String slot,String id,String type){
        if(id==null||id.isBlank())throw bad("展示对象不能为空");DashboardSelections old=get();
        return switch(slot.toLowerCase()){
            case "dream"->{if(dreams.find(id).isEmpty())throw missing();yield values.save(new DashboardSelections(id,old.ritualId(),old.mediaType(),old.mediaId()));}
            case "ritual"->{if(rituals.find(id).isEmpty())throw missing();yield values.save(new DashboardSelections(old.dreamId(),id,old.mediaType(),old.mediaId()));}
            case "media"->{String kind=type==null?"":type.toUpperCase();boolean found=switch(kind){case "ANIME"->media.anime().stream().anyMatch(v->v.id().equals(id));case "GAME"->media.games().stream().anyMatch(v->v.id().equals(id));default->media.items().stream().anyMatch(v->v.id().equals(id));};if(!found)throw missing();yield values.save(new DashboardSelections(old.dreamId(),old.ritualId(),kind,id));}
            default->throw bad("未知的 Dashboard 展示位");
        };
    }
    private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"展示对象不存在");}
    private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
