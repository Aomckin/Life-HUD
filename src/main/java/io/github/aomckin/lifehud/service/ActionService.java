package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.aomckin.lifehud.core.ActionCatalog;
import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.domain.ActionDurationOption;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import io.github.aomckin.lifehud.repository.LogRepository;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public final class ActionService {
    private final Player player;
    private final ActionCatalog actions;
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final TitleSystem titles;
    private final ProgressionService progression;
    private final LogRepository logs;
    private final GameQueryService queries;

    public ActionService(Player player, ActionCatalog actions, PlayerRepository playerRepository,
                         PlayerService playerService, TitleSystem titles, ProgressionService progression,
                         LogRepository logs, GameQueryService queries) {
        this.player = player; this.actions = actions; this.playerRepository = playerRepository;
        this.playerService = playerService; this.titles = titles; this.progression = progression;
        this.logs = logs; this.queries = queries;
    }

    public int actionExp(JsonNode info) {
        double value = info.path("exp_change").asDouble(); return value >= 0 ? (int) value : 0;
    }
    private boolean canEnergy(int change) { return change >= 0 || player.energy + change >= 0; }

    public OperationResult completeNow(String name) {
        if (!actions.contains(name)) return queries.error("行动不存在");
        int before = player.exp; JsonNode info = actions.get(name);
        int change = titles.applyActionEnergyBonus(name, info.path("energy_change").asInt());
        int exp = actionExp(info); if (!canEnergy(change)) return queries.error("能量不足");
        playerService.completeAction(player, name, change, exp); playerRepository.save(player);
        logs.action("执行行动：" + name, String.format("能量%+d", change), player.energy);
        return progression.result(true, "行动完成", before, List.of());
    }

    public OperationResult completeTimed(String name, ActionDurationOption requested) {
        if (!actions.contains(name)) return queries.error("行动不存在");
        ActionDurationOption option = actions.allowedOption(name, requested);
        if (option == null) return queries.error("时长选项无效");
        int before = player.exp; JsonNode info = actions.get(name);
        int energy = titles.applyActionEnergyBonus(name,
                pythonRound(info.path("energy_change").asDouble() * option.multiplier()));
        if (!canEnergy(energy)) return queries.error("能量不足");
        int exp = pythonRound(actionExp(info) * option.multiplier());
        Map<String, Object> result = playerService.completeTimedAction(player, name, option.minutes(), energy, exp);
        playerRepository.save(player); logs.timedAction(name, option.minutes(), energy, exp, player.energy);
        return progression.result(true, "行动完成", before, List.of(new GameEvent(
                GameEvents.ACTION_COMPLETE, GameViewAssembler.map("action_result", result))));
    }

    private int pythonRound(double value) { return (int) Math.rint(value); }
}
