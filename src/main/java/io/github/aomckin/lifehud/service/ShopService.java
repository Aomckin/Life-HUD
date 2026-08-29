package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import io.github.aomckin.lifehud.repository.LogRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
@Deprecated(forRemoval = false)
public final class ShopService {
    private final ShopManager shop;
    private final DailyTaskManager dailyTasks;
    private final SpecialTaskManager specialTasks;
    private final LogRepository logs;
    private final GameQueryService queries;

    public ShopService(ShopManager shop, DailyTaskManager dailyTasks, SpecialTaskManager specialTasks,
                       LogRepository logs, GameQueryService queries) {
        this.shop = shop; this.dailyTasks = dailyTasks; this.specialTasks = specialTasks;
        this.logs = logs; this.queries = queries;
    }

    public OperationResult buy(String id) {
        Map<String, Object> result = shop.buy(id);
        if (!(boolean) result.get("success")) return queries.error(Objects.toString(result.getOrDefault("message", "购买失败")));
        @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) result.get("item");
        logs.action("购买商品：" + item.getOrDefault("name", id), "金币-" + item.getOrDefault("price", 0), shop.energy());
        String effect = Objects.toString(result.get("effect_type"), "");
        if (effect.equals("refresh_daily_tasks")) dailyTasks.redraw();
        if (effect.equals("special_task_slot")) specialTasks.setSlotCount(shop.specialTaskSlots());
        return new OperationResult(true, Objects.toString(result.getOrDefault("message", "购买成功")),
                List.of(new GameEvent(GameEvents.SHOP_PURCHASE,
                        GameViewAssembler.map("item", item, "effect_type", effect))), queries.state());
    }
}
