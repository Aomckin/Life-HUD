package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

public final class ShopManager {
    private final Player player;private final PlayerRepository players;private final PlayerService playerService;private final Supplier<LocalDate> today;private final List<String> categories;private final List<Map<String,Object>> items;
    @SuppressWarnings("unchecked") public ShopManager(Player p,PlayerRepository players,JsonFileStore repo,ObjectMapper mapper,PlayerService playerService){this(p,players,repo,mapper,playerService,LocalDate::now);}
    @SuppressWarnings("unchecked") public ShopManager(Player p,PlayerRepository players,JsonFileStore repo,ObjectMapper mapper,PlayerService playerService,Supplier<LocalDate> today){this.player=p;this.players=players;this.today=today;this.playerService=playerService;Map<String,Object> data=mapper.convertValue(repo.read("shop.json"),Map.class);this.categories=(List<String>)data.getOrDefault("categories",List.of("任务","成长","称号","娱乐","收藏"));this.items=(List<Map<String,Object>>)data.getOrDefault("items",List.of());}
    public Map<String,Object> item(String id){return items.stream().filter(i->id.equals(i.get("id"))).findFirst().orElse(null);}public List<Map<String,Object>> byCategory(String c){return items.stream().filter(i->c.equals(i.get("category"))).toList();}public boolean isUnlocked(Map<String,Object> item){return true;}
    public int dailyCount(String id){return player.shop_daily_purchases.getOrDefault(today.get().toString(),Map.of()).getOrDefault(id,0);}public int totalCount(String id){return player.shop_total_purchases.getOrDefault(id,0);}
    public boolean soldOut(Map<String,Object> i){String stock=str(i.getOrDefault("stock_type","infinite")),id=str(i.get("id"));return stock.equals("daily")?dailyCount(id)>=1:stock.equals("permanent")&&totalCount(id)>=1;}
    public boolean canBuy(String id){var i=item(id);return i!=null&&isUnlocked(i)&&!soldOut(i)&&player.coin>=num(i.get("price"));}
    public String stockText(Map<String,Object> i){return switch(str(i.getOrDefault("stock_type","infinite"))){case"daily"->"每日限购";case"permanent"->"永久限购";default->"无限供应";};}
    public String buttonText(Map<String,Object> i){if(!isUnlocked(i))return"未解禁";if(soldOut(i))return str(i.get("stock_type")).equals("daily")?"今日已购":str(i.get("stock_type")).equals("permanent")?"已购买":"售罄";if(player.coin<num(i.get("price")))return"金币不足";return"购买";}
    public Map<String,Object> buy(String id){var i=item(id);if(i==null)return result(false,"商品不存在");if(!isUnlocked(i))return result(false,"商品未解禁");if(soldOut(i))return result(false,"商品已达购买上限");if(!playerService.spendCoin(player,num(i.get("price"))))return result(false,"金币不足");playerService.recordShopPurchase(player,i,today.get().toString());playerService.applyShopEffect(player,i,today.get().toString());players.save(player);var r=result(true,"购买成功");r.put("item",i);r.put("effect_type",i.get("effect_type"));r.put("effect_value",i.getOrDefault("effect_value",0));return r;}
    public int taskExpMultiplier(){return playerService.hasDailyDoubleExp(player,today.get().toString())?2:1;}public int energy(){return player.energy;}public int specialTaskSlots(){return player.special_task_slots;}public List<String> categories(){return categories;}public List<Map<String,Object>> items(){return items;}public String today(){return today.get().toString();}
    private static Map<String,Object> result(boolean ok,String message){Map<String,Object> m=new LinkedHashMap<>();m.put("success",ok);m.put("message",message);return m;}private static int num(Object o){return o instanceof Number n?n.intValue():0;}private static String str(Object o){return Objects.toString(o,"");}
}
