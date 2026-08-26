package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.*;
import java.util.*;
public final class TitleSystem extends UnlockableSystem {
    public static final Set<String> OUTDOOR_DAILY_TASK_IDS=Set.of("walk_20_minutes"),OUTDOOR_SPECIAL_TASK_IDS=Set.of("wind_traveler","city_roaming","face_inner_self","summer_night_walk"),NEGATIVE_ACTION_IDS=Set.of("看番","推gal","打游戏");
    private final Player player;private final PlayerRepository players;private final JsonFileStore repo;private final PlayerService playerService;
    @SuppressWarnings("unchecked") public TitleSystem(Player p,PlayerRepository players,JsonFileStore repo,ObjectMapper mapper,PlayerService playerService){super(mapper.convertValue(repo.read("titles.json"),List.class));this.player=p;this.players=players;this.repo=repo;this.playerService=playerService;}
    protected Collection<String> unlockedIds(){return player.unlocked_titles;}
    @SuppressWarnings("unchecked") protected boolean isDone(Map<String,Object> title,Object context){String type=str(title.get("condition_type"));Object target=title.get("target_value");List<DailyTask> tasks=context instanceof List<?> l?(List<DailyTask>)l:List.of();if(type.equals("always"))return true;if(target==null)return false;return switch(type){
        case"task_completed_count"->tasks.stream().mapToInt(t->t.completedCount).sum()>=num(target);case"task_done_count"->player.done_task_count>=num(target);case"energy_reach"->player.energy>=num(target);case"action_count"->{var pair=parseActionTarget(title);yield !pair.id.isBlank()&&player.action_counts.getOrDefault(pair.id,0)>=pair.count;}case"action_combo_count"->actionCombo(target);case"special_task_completed_count"->{Map<String,Object> m=(Map<String,Object>)target;yield specialCount(str(m.get("task_id")))>=numOr(m.get("count"),1);}case"achievement_unlocked_count"->player.unlocked_achievements.size()>=num(target);case"timed_action_completed_count"->player.completed_timed_actions>=num(target);case"outdoor_task_completed_count"->outdoorCount(tasks)>=num(target);case"shop_purchase"->player.shop_total_purchases.getOrDefault(str(target),0)>0;default->false;};}
    @SuppressWarnings("unchecked") private boolean actionCombo(Object target){if(!(target instanceof List<?> list))return false;for(Object o:list){Map<String,Object> m=(Map<String,Object>)o;String id=str(m.get("action_id"));if(id.isBlank()||player.action_counts.getOrDefault(id,0)<numOr(m.get("count"),1))return false;}return true;}
    private record ActionTarget(String id,int count){} @SuppressWarnings("unchecked") private ActionTarget parseActionTarget(Map<String,Object> t){Object v=t.get("target_value");return v instanceof Map<?,?> m?new ActionTarget(str(m.get("action_id")),numOr(m.get("count"),1)):new ActionTarget(str(t.get("target_action")),num(v));}
    private int specialCount(String id){for(var t:repo.read("special_tasks.json").path("tasks"))if(id.equals(t.path("id").asText()))return t.path("completed_count").asInt();return 0;}
    private int outdoorCount(List<DailyTask> daily){int total=daily.stream().filter(t->OUTDOOR_DAILY_TASK_IDS.contains(t.id)).mapToInt(t->t.completedCount).sum();for(String id:OUTDOOR_SPECIAL_TASK_IDS)total+=specialCount(id);return total;}
    public List<Map<String,Object>> checkTitles(List<DailyTask> tasks){return check(tasks);} public Map<String,Object> byId(String id){return items.stream().filter(t->id.equals(t.get("id"))).findFirst().orElse(null);}
    public boolean unlockById(String id){if(byId(id)==null)return false;playerService.unlockTitle(player,id);if(player.equipped_title.isBlank())playerService.equipTitle(player,id);players.save(player);return true;}
    public boolean equip(String id){if(!player.unlocked_titles.contains(id)||byId(id)==null)return false;playerService.equipTitle(player,id);players.save(player);return true;}
    public Map<String,Object> equipped(){Map<String,Object> t=byId(player.equipped_title);if(t!=null)return t;Map<String,Object> none=new LinkedHashMap<>();none.put("id","");none.put("name","无称号");none.put("desc","还没有佩戴称号。");none.put("bonus_type","none");none.put("bonus_value",0);return none;}
    public String name(){return str(equipped().getOrDefault("name","无称号"));} public double expBonusRate(){return str(equipped().get("bonus_type")).equals("exp_rate")?dbl(equipped().get("bonus_value")):0;}
    public String bonusText(){return bonusText(equipped());} @SuppressWarnings("unchecked") public String bonusText(Map<String,Object> t){String type=str(t.get("bonus_type"));Object v=t.getOrDefault("bonus_value",0);return switch(type){case"exp_rate"->pct("经验 +",v);case"energy_change_rate"->pct("能量变化 +",v);case"positive_energy_rate"->pct("正向行动能量收益 +",v);case"negative_action_exp_rate"->pct("娱乐行动经验 +",v);case"special_task_exp_rate"->pct("特殊任务经验 +",v);case"outdoor_task_exp_rate"->pct("户外任务经验 +",v);case"action_energy_rate"->{Map<String,Object> m=(Map<String,Object>)v;yield str(m.getOrDefault("action_id","指定行动"))+"能量收益 +"+(int)(dbl(m.get("rate"))*100)+"%";}case"action_energy_cost_reduction"->{Map<String,Object> m=(Map<String,Object>)v;yield str(m.getOrDefault("action_id","指定行动"))+"能量消耗 -"+(int)(dbl(m.get("rate"))*100)+"%";}default->"无加成";};}
    @SuppressWarnings("unchecked") public String conditionText(Map<String,Object> t){String type=str(t.get("condition_type"));Object v=t.get("target_value");return switch(type){case"always"->"默认解锁";case"task_completed_count"->"累计完成普通任务 "+v+" 次";case"task_done_count"->"完成任务 "+v+" 次";case"energy_reach"->"能量达到 "+v;case"action_count"->{var p=parseActionTarget(t);yield p.id+"累计完成 "+p.count+" 次";}case"action_combo_count"->{List<String> parts=new ArrayList<>();for(var r:(List<Map<String,Object>>)v)parts.add(str(r.getOrDefault("action_id","指定行动"))+" "+numOr(r.get("count"),1)+" 次");yield String.join("、",parts);}case"special_task_completed_count"->{Map<String,Object> m=(Map<String,Object>)v;yield"完成特殊任务 "+m.get("task_id")+" "+numOr(m.get("count"),1)+" 次";}case"achievement_unlocked_count"->"解锁成就 "+v+" 个";case"timed_action_completed_count"->"完成计时行动 "+v+" 次";case"outdoor_task_completed_count"->"累计完成户外相关任务 "+v+" 次";case"shop_purchase"->"在商店购买 "+v;default->"未知条件";};}
    public int applyTaskExpBonus(int exp, String taskId, TaskSource source) {
        Map<String, Object> title = equipped();
        String type = str(title.get("bonus_type"));
        double rate = dbl(title.get("bonus_value"));
        if (type.equals("exp_rate")
                || type.equals("special_task_exp_rate") && source == TaskSource.SPECIAL
                || type.equals("outdoor_task_exp_rate") && isOutdoor(taskId, source)) {
            return rateBonus(exp, rate);
        }
        return exp;
    }
    public int applyTaskExpBonus(int exp, String taskId, String source) {
        return applyTaskExpBonus(exp, taskId, TaskSource.fromJson(source));
    }
    public int applyActionExpBonus(String action,int exp){Map<String,Object> t=equipped();return str(t.get("bonus_type")).equals("negative_action_exp_rate")&&NEGATIVE_ACTION_IDS.contains(action)?rateBonus(exp,dbl(t.get("bonus_value"))):exp;}
    @SuppressWarnings("unchecked") public int applyActionEnergyBonus(String action,int change){Map<String,Object> t=equipped();String type=str(t.get("bonus_type"));Object value=t.getOrDefault("bonus_value",0);if(type.equals("energy_change_rate")||type.equals("positive_energy_rate")&&change>0)return signedRate(change,dbl(value));if(type.equals("action_energy_rate")&&change>0&&value instanceof Map<?,?> raw){Map<String,Object> m=(Map<String,Object>)raw;if(str(m.get("action_id")).equals(action))return signedRate(change,dbl(m.get("rate")));}if(type.equals("action_energy_cost_reduction")&&change<0&&value instanceof Map<?,?> raw){Map<String,Object> m=(Map<String,Object>)raw;if(str(m.get("action_id")).equals(action))return costReduction(change,dbl(m.get("rate")));}return change;}
    public int applyEnergyBonus(int change){return applyActionEnergyBonus("",change);}public int rateBonus(int value,double rate){if(rate<=0||value==0)return value;int bonus=(int)(value*rate);return value+(bonus==0?1:bonus);}public int signedRate(int change,double rate){if(rate<=0||change==0)return change;return(change>0?1:-1)*(int)(Math.abs(change)*(1+rate)+0.5);}public int costReduction(int change,double rate){return rate<=0||change>=0?change:-(int)(Math.abs(change)*(1-rate)+0.5);}public boolean isOutdoor(String id, TaskSource source) {
        return source == TaskSource.DAILY ? OUTDOOR_DAILY_TASK_IDS.contains(id)
                : OUTDOOR_SPECIAL_TASK_IDS.contains(id);
    }
    private static int num(Object o){return o instanceof Number n?n.intValue():0;}private static int numOr(Object o,int d){return o instanceof Number n?n.intValue():d;}private static double dbl(Object o){return o instanceof Number n?n.doubleValue():0;}private static String str(Object o){return Objects.toString(o,"");}private static String pct(String prefix,Object v){return prefix+(int)(dbl(v)*100)+"%";}
}
