package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.DashboardSummary;
import io.github.aomckin.lifehud.service.AgentContextService;
import io.github.aomckin.lifehud.domain.DashboardHeadline;
import io.github.aomckin.lifehud.service.DashboardHeadlineService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public final class DashboardController {
    private final AgentContextService context;private final DashboardHeadlineService headlines;
    public DashboardController(AgentContextService context,DashboardHeadlineService headlines){this.context=context;this.headlines=headlines;}
    @GetMapping public DashboardSummary summary(){return context.dashboard();}
    @GetMapping("/headlines") public java.util.List<DashboardHeadline> headlines(){return headlines.all();}
    @PostMapping("/headlines") public DashboardHeadline add(@RequestBody HeadlineRequest request){return headlines.add(request==null?null:request.text());}
    @DeleteMapping("/headlines/{id}") public void remove(@PathVariable String id){headlines.remove(id);}
    public record HeadlineRequest(String text) { }
}
