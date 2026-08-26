package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.*;
import io.github.aomckin.lifehud.core.GameCommandFacade;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class GameController {
    private final GameCommandFacade game;
    public GameController(GameCommandFacade game){this.game=game;}
    @GetMapping("/state") public Map<String,Object> state(){return game.state();}
    @GetMapping("/actions/{actionName}/duration-options") public OperationResult duration(@PathVariable String actionName){return game.durationOptions(actionName);}
    @PostMapping("/command") public OperationResult command(@RequestBody GameCommand command){return game.execute(command);}
}
