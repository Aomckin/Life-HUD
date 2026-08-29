package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public final class GrowthTitleService {
    private static final String FILE="custom-titles.json";
    private static final TypeReference<List<GrowthCatalog.TitleDefinition>> TYPE=new TypeReference<>() { };
    private final Player player;private final PlayerRepository players;private final JsonFileStore files;private final ObjectMapper mapper;private final LifeEventService events;
    public GrowthTitleService(Player player,PlayerRepository players,JsonFileStore files,ObjectMapper mapper,LifeEventService events){this.player=player;this.players=players;this.files=files;this.mapper=mapper;this.events=events;}
    public List<Map<String,Object>> all(){List<GrowthCatalog.TitleDefinition> definitions=new ArrayList<>(GrowthCatalog.TITLES);definitions.addAll(custom());Set<String> known=new HashSet<>();definitions.forEach(v->known.add(v.id()));if(files.exists("titles.json"))for(var node:files.read("titles.json")){String id=node.path("id").asText();if(player.unlocked_titles.contains(id)&&known.add(id))definitions.add(new GrowthCatalog.TitleDefinition(id,node.path("name").asText(id),node.path("desc").asText("从旧版 Life HUD 保留下来的称号。"),"SYSTEM","migration",false,false));}for(String legacy:player.unlocked_titles)if(known.add(legacy))definitions.add(new GrowthCatalog.TitleDefinition(legacy,legacy,"从旧版 Life HUD 保留下来的称号。","SYSTEM","migration",false,false));return definitions.stream().map(this::view).toList();}
    public synchronized Map<String,Object> create(TitleRequest request){String name=request==null?"":clean(request.name());if(name.isBlank())throw bad("称号名称不能为空");GrowthCatalog.TitleDefinition value=new GrowthCatalog.TitleDefinition(UUID.randomUUID().toString(),name,clean(request.description()),"MANUAL","",false,true);List<GrowthCatalog.TitleDefinition> custom=new ArrayList<>(custom());custom.add(value);files.write(FILE,custom);if(!player.unlocked_titles.contains(value.id()))player.unlocked_titles.add(value.id());players.save(player);events.record(LifeEventType.TITLE_UNLOCKED,"title",value.name(),value.description(),List.of("growth","title"),Map.of("titleId",value.id(),"custom",true));return view(value);}
    public synchronized Map<String,Object> equip(String id){Map<String,Object> title=all().stream().filter(v->id.equals(v.get("id"))).findFirst().orElseThrow(()->bad("称号不存在"));if(!player.unlocked_titles.contains(id))throw bad("称号尚未解锁");player.equipped_title=id;players.save(player);events.record(LifeEventType.TITLE_EQUIPPED,"title",String.valueOf(title.get("name")),"设为当前称号",List.of("growth","title"),Map.of("titleId",id));return title;}
    public synchronized void unequip(){if(player.equipped_title.isBlank())return;String id=player.equipped_title;player.equipped_title="";players.save(player);events.record(LifeEventType.TITLE_EQUIPPED,"title","取消当前称号","已取消装备称号",List.of("growth","title"),Map.of("titleId",id,"equipped",false));}
    public synchronized void delete(String id){List<GrowthCatalog.TitleDefinition> custom=new ArrayList<>(custom());if(custom.stream().noneMatch(v->v.id().equals(id)))throw bad("系统称号不能删除");custom.removeIf(v->v.id().equals(id));files.write(FILE,custom);player.unlocked_titles.remove(id);if(id.equals(player.equipped_title))player.equipped_title="";players.save(player);}
    public String currentName(){return all().stream().filter(v->Boolean.TRUE.equals(v.get("equipped"))).map(v->String.valueOf(v.get("name"))).findFirst().orElse("尚未选择");}
    private Map<String,Object> view(GrowthCatalog.TitleDefinition t){boolean unlocked=player.unlocked_titles.contains(t.id()),equipped=t.id().equals(player.equipped_title);Map<String,Object> map=new LinkedHashMap<>();map.put("id",t.id());map.put("name",t.hidden()&&!unlocked?"隐藏称号":t.name());map.put("description",t.hidden()&&!unlocked?"尚未遇见。":t.description());map.put("sourceType",t.sourceType());map.put("sourceId",t.sourceId());map.put("hidden",t.hidden());map.put("custom",t.custom());map.put("unlocked",unlocked);map.put("equipped",equipped);map.put("unlockedAt",unlocked?unlockTime(t.id()):null);return map;}
    private Instant unlockTime(String id){return events.all().stream().filter(e->e.type()==LifeEventType.TITLE_UNLOCKED&&id.equals(String.valueOf(e.metadata().get("titleId")))).map(LifeEvent::occurredAt).findFirst().orElse(null);}
    private List<GrowthCatalog.TitleDefinition> custom(){if(!files.exists(FILE))return List.of();try{return mapper.convertValue(files.read(FILE),TYPE);}catch(IllegalArgumentException e){throw new IllegalStateException("无法读取自定义称号",e);}}
    private String clean(String value){return value==null?"":value.trim();}private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
