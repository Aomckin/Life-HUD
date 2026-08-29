package io.github.aomckin.lifehud.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.github.aomckin.lifehud.domain.ActionDurationOption;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.dto.GameCommand;
import io.github.aomckin.lifehud.dto.OperationResult;
import io.github.aomckin.lifehud.repository.LogRepository;
import io.github.aomckin.lifehud.service.ActionService;
import io.github.aomckin.lifehud.service.GameQueryService;
import io.github.aomckin.lifehud.service.ProgressionService;
import io.github.aomckin.lifehud.service.ShopService;
import io.github.aomckin.lifehud.service.TaskService;
import io.github.aomckin.lifehud.service.TitleService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Application command boundary: identify a command and delegate the use case. */
@Component
public class GameCommandFacade {
    private final ActionService actionService; private final TaskService taskService;
    private final ShopService shopService; private final TitleService titleService;
    private final ProgressionService progressionService; private final GameQueryService queries;
    private final Player player; private final LogRepository logs;

    public GameCommandFacade(ActionService actionService, TaskService taskService, ShopService shopService,
                             TitleService titleService, ProgressionService progressionService,
                             GameQueryService queries, Player player, LogRepository logs) {
        this.actionService = actionService; this.taskService = taskService; this.shopService = shopService;
        this.titleService = titleService; this.progressionService = progressionService; this.queries = queries;
        this.player = player; this.logs = logs;
    }

    public java.util.Map<String, Object> state() { return queries.state(); }
    public OperationResult durationOptions(String name) { return queries.durationOptions(name); }

    public synchronized OperationResult execute(GameCommand command) {
        JsonNode payload = command.payload() == null ? MissingNode.getInstance() : command.payload();
        if (command.type() == null) return queries.error("未知指令");
        return switch (command.type()) {
            case GameCommands.INITIALIZE_PROGRESSION -> progressionService.result(true, "", player.exp, List.of());
            case GameCommands.COMPLETE_ACTION -> actionService.completeNow(payload.path("action_name").asText());
            case GameCommands.COMPLETE_TIMED_ACTION -> actionService.completeTimed(
                    payload.path("action_name").asText(), toOption(payload.path("option")));
            case GameCommands.COMPLETE_DAILY_TASK -> taskService.completeDaily(payload.path("index").asInt(-1));
            case GameCommands.COMPLETE_SPECIAL_TASK -> taskService.completeSpecial(payload.path("index").asInt(-1));
            case GameCommands.BUY_SHOP_ITEM -> queries.error("v0.4 已停用商店与金币经济");
            case GameCommands.EQUIP_TITLE -> titleService.equip(payload.path("title_id").asText());
            case GameCommands.REFRESH_DAILY_TASKS -> taskService.refresh();
            case GameCommands.LOG_ABANDONED_ACTION -> {
                logs.abandon(payload.path("action_name").asText(), payload.path("elapsed_minutes").asInt());
                yield new OperationResult(true, "已记录放弃行动", List.of(), state());
            }
            default -> queries.error("未知指令");
        };
    }

    private ActionDurationOption toOption(JsonNode node) {
        return node == null || !node.isObject() ? null : new ActionDurationOption(
                node.path("minutes").asInt(), node.path("multiplier").asDouble(1), node.path("exp").asInt());
    }
}
