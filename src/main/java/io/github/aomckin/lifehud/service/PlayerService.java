package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.Player;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class PlayerService {
    public int energy(Player p){return p.energy;} public int maxEnergy(Player p){return p.maxEnergy;} public int exp(Player p){return p.exp;} public int coin(Player p){return p.coin;}
    public Map<String,Integer> actionCounts(Player p){return p.action_counts;} public int doneTaskCount(Player p){return p.done_task_count;} public int doneSpecialTaskCount(Player p){return p.done_special_task_count;}
    public Set<String> unlockedAchievementIds(Player p){return new LinkedHashSet<>(p.unlocked_achievements);} public Set<String> unlockedTitleIds(Player p){return new LinkedHashSet<>(p.unlocked_titles);}
    public String equippedTitleId(Player p){return p.equipped_title;} public int specialTaskSlots(Player p){return p.special_task_slots;} public boolean hasDailyDoubleExp(Player p,String today){return today.equals(p.daily_double_exp_date);}
    public void clampEnergy(Player p){p.energy=Math.max(0,Math.min(p.maxEnergy,p.energy));} public void addEnergy(Player p,int v){p.energy+=v;clampEnergy(p);} public void addExp(Player p,int v){p.exp+=v;} public void addCoin(Player p,int v){p.coin+=v;}
    public boolean spendCoin(Player p,int v){if(p.coin<v)return false;p.coin-=v;return true;}
    public void unlockAchievement(Player p,String id){if(!p.unlocked_achievements.contains(id))p.unlocked_achievements.add(id);} public void unlockTitle(Player p,String id){if(!p.unlocked_titles.contains(id))p.unlocked_titles.add(id);} public void equipTitle(Player p,String id){p.equipped_title=id;}
    public void incrementAction(Player p,String name){p.action_counts.merge(name,1,Integer::sum);} public void incrementTaskDone(Player p){p.done_task_count++;} public void incrementSpecialTaskDone(Player p){p.done_special_task_count++;} public void incrementTimedAction(Player p){p.completed_timed_actions++;}
    public void addDailyTaskReward(Player p,int energy,int exp){addEnergy(p,energy);addExp(p,exp);incrementTaskDone(p);} public void addSpecialTaskReward(Player p,int coin,int exp){addCoin(p,coin);addExp(p,exp);incrementSpecialTaskDone(p);}
    public void addActionResult(Player p,String name,int energy,int exp){addEnergy(p,energy);addExp(p,exp);incrementAction(p,name);} public void completeAction(Player p,String name,int energy,int exp){addActionResult(p,name,energy,exp);}
    public Map<String,Object> completeTimedAction(Player p,String name,int minutes,int energy,int exp){addActionResult(p,name,energy,0);addExp(p,exp);incrementTimedAction(p);Map<String,Object> r=new LinkedHashMap<>();r.put("action_name",name);r.put("duration_minutes",minutes);r.put("energy_change",energy);r.put("exp_change",exp);r.put("current_energy",p.energy);return r;}
    public void recordShopPurchase(Player p,Map<String,Object> item,String today){String id=Objects.toString(item.get("id"),"");String stock=Objects.toString(item.getOrDefault("stock_type","infinite"));if(stock.equals("daily"))p.shop_daily_purchases.computeIfAbsent(today,k->new LinkedHashMap<>()).merge(id,1,Integer::sum);if(stock.equals("permanent"))p.shop_total_purchases.merge(id,1,Integer::sum);}
    public void applyShopEffect(Player p,Map<String,Object> item,String today){String effect=Objects.toString(item.get("effect_type"),"");Object v=item.getOrDefault("effect_value",0);if(effect.equals("daily_double_exp"))p.daily_double_exp_date=today;if(effect.equals("special_task_slot"))p.special_task_slots+=((Number)v).intValue();if(effect.equals("unlock_title"))unlockTitle(p,Objects.toString(v));}
}
