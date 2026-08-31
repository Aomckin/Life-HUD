package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.DashboardSummary;
import io.github.aomckin.lifehud.service.AgentContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public final class DashboardController {
    private final AgentContextService context;
    public DashboardController(AgentContextService context){this.context=context;}
    @GetMapping public DashboardSummary summary(){return context.dashboard();}
}
