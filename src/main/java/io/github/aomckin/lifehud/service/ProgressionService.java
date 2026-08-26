package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.GameEvents;
import io.github.aomckin.lifehud.dto.GameEvent;
import io.github.aomckin.lifehud.dto.OperationResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Coordinates cross-cutting progression checks after a successful command. */
@Service
public final class ProgressionService {
    private final AchievementService checks;
    private final LevelService levels;
    private final GameQueryService queries;
    private final GameViewAssembler views;

    public ProgressionService(AchievementService checks, LevelService levels, GameQueryService queries,
                              GameViewAssembler views) {
        this.checks = checks; this.levels = levels; this.queries = queries; this.views = views;
    }

    public OperationResult result(boolean success, String message, int beforeExp, List<GameEvent> initial) {
        List<GameEvent> events = new ArrayList<>(initial == null ? List.of() : initial);
        for (var achievement : checks.checkAchievements()) {
            events.add(new GameEvent(GameEvents.ACHIEVEMENT_UNLOCK,
                    GameViewAssembler.map("achievement", views.achievementEvent(achievement))));
        }
        for (var title : checks.checkTitles()) {
            events.add(new GameEvent(GameEvents.TITLE_UNLOCK, GameViewAssembler.map("title", title)));
        }
        var level = levels.levelUpInfo(beforeExp, checks.currentExp());
        if (level != null) events.add(new GameEvent(GameEvents.LEVEL_UP,
                GameViewAssembler.map("level_up_info", level)));
        return new OperationResult(success, message, events, queries.state());
    }
}
