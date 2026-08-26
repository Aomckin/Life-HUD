package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LevelService {
    private final JsonNode config;
    public LevelService(JsonNode config){this.config=config;}
    public Map<String,Object> status(int totalExp){int level=config.path("initial_level").asInt(),max=config.path("max_level").asInt(),remaining=totalExp;while(level<max){int required=requiredExp(level);if(remaining<required){Map<String,Object> m=new LinkedHashMap<>();m.put("level",level);m.put("current_exp",remaining);m.put("required_exp",required);m.put("is_max_level",false);return m;}remaining-=required;level++;}Map<String,Object> m=new LinkedHashMap<>();m.put("level",max);m.put("current_exp",0);m.put("required_exp",0);m.put("is_max_level",true);return m;}
    public int requiredExp(int level){int initial=config.path("initial_level").asInt();return config.path("base_required_exp").asInt()+((level-initial)/config.path("step_levels").asInt())*config.path("step_increase_exp").asInt();}
    public int level(int totalExp){return (int)status(totalExp).get("level");}
    public String expText(int totalExp){var s=status(totalExp);return (boolean)s.get("is_max_level")?"经验：满级":"经验："+s.get("current_exp")+"/"+s.get("required_exp");}
    public String levelText(int totalExp){return "等级：Lv."+level(totalExp);}
    public Map<String,Object> levelUpInfo(int before,int after){int a=level(before),b=level(after);if(b<=a)return null;Map<String,Object> m=new LinkedHashMap<>();m.put("before_level",a);m.put("after_level",b);return m;}
}
