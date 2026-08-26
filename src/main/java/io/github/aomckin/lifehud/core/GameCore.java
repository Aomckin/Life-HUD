package io.github.aomckin.lifehud.core;

import io.github.aomckin.lifehud.domain.ActionDurationOption;
import io.github.aomckin.lifehud.service.ActionService;
import io.github.aomckin.lifehud.service.ProgressionService;
import io.github.aomckin.lifehud.service.ShopService;
import io.github.aomckin.lifehud.service.TaskService;
import io.github.aomckin.lifehud.service.TitleService;
import io.github.aomckin.lifehud.service.GameQueryService;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.LogRepository;
import java.util.List;

/** @deprecated Use GameCommandFacade. Kept as a source-compatible command alias only. */
@Deprecated
public class GameCore extends GameCommandFacade {
    public static final List<ActionDurationOption> POSITIVE_OPTIONS = ActionCatalog.POSITIVE_OPTIONS;
    public static final List<ActionDurationOption> NEGATIVE_OPTIONS = ActionCatalog.NEGATIVE_OPTIONS;

    public GameCore(ActionService actionService, TaskService taskService, ShopService shopService,
                    TitleService titleService, ProgressionService progressionService,
                    GameQueryService queries, Player player, LogRepository logs) {
        super(actionService, taskService, shopService, titleService, progressionService, queries, player, logs);
    }
}
