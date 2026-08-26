package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.ActionCatalog;
import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import java.util.List;

/** Read-only application query boundary. It never mutates game state. */
public final class GameQueryService {
    private final ActionCatalog actions;
    private final GameViewAssembler assembler;

    public GameQueryService(ActionCatalog actions, GameViewAssembler assembler) {
        this.actions = actions;
        this.assembler = assembler;
    }

    public java.util.Map<String, Object> state() { return assembler.assemble().asMap(); }

    public OperationResult durationOptions(String name) {
        if (!actions.contains(name)) return error("行动不存在");
        return new OperationResult(true, "", List.of(new GameEvent(
                GameEvents.ACTION_DURATION_OPTIONS,
                GameViewAssembler.map("options", actions.durationOptions(actions.get(name))))), state());
    }

    public OperationResult error(String message) {
        return new OperationResult(false, message, List.of(new GameEvent(
                GameEvents.ERROR, GameViewAssembler.map("message", message))), state());
    }
}
