package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.agent.AgentContext;
import io.github.aomckin.lifehud.service.AgentContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/context")
public final class AgentContextController {
    private final AgentContextService context;
    public AgentContextController(AgentContextService context){this.context=context;}
    @GetMapping("/today") public AgentContext.Today today(){return context.today();}
    @GetMapping("/recent") public AgentContext.Recent recent(@RequestParam(defaultValue="7") int days){return context.recent(days);}
    @GetMapping("/status") public AgentContext.StatusResponse status(){return context.statusResponse();}
    @GetMapping("/focus") public AgentContext.FocusResponse focus(){return context.focusResponse();}
    @GetMapping("/tasks") public AgentContext.TasksResponse tasks(){return context.tasksResponse();}
    @GetMapping("/dreams") public AgentContext.DreamsResponse dreams(){return context.dreamsResponse();}
    @GetMapping("/life") public AgentContext.LifeResponse life(){return context.lifeResponse();}
    @GetMapping("/journal") public AgentContext.JournalResponse journal(@RequestParam(defaultValue="20") int limit){return context.journalResponse(limit);}
    @GetMapping("/media") public AgentContext.MediaResponse media(){return context.mediaResponse();}
    @GetMapping("/growth") public AgentContext.GrowthResponse growth(){return context.growthResponse();}
}
