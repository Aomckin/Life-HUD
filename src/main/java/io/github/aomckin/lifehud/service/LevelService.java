package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LevelService {
    private final JsonNode config;
    public LevelService(JsonNode config){this.config=config;}
    public Map<String,Object> status(int totalExp){int level=config.path("initial_level").asInt(1),remaining=Math.max(0,totalExp);while(remaining>=requiredExp(level)&&level<10000){remaining-=requiredExp(level);level++;}Map<String,Object> m=new LinkedHashMap<>();m.put("level",level);m.put("current_exp",remaining);m.put("required_exp",requiredExp(level));m.put("is_max_level",false);return m;}
    /** Preserves the historical curve while removing the old hard level cap. */
    public int requiredExp(int level){int initial=config.path("initial_level").asInt(1);return config.path("base_required_exp").asInt(20)+((Math.max(initial,level)-initial)/Math.max(1,config.path("step_levels").asInt(5)))*config.path("step_increase_exp").asInt(10);}
    public int level(int totalExp){return (int)status(totalExp).get("level");}
    public String expText(int totalExp){var s=status(totalExp);return "EXP："+s.get("current_exp")+"/"+s.get("required_exp");}
    public String levelText(int totalExp){return "等级：Lv."+level(totalExp);}
    public Map<String,Object> levelUpInfo(int before,int after){int a=level(before),b=level(after);if(b<=a)return null;Map<String,Object> m=new LinkedHashMap<>();m.put("before_level",a);m.put("after_level",b);return m;}
}
