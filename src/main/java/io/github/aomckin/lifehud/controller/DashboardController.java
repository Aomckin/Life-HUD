package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.DashboardSummary;
import io.github.aomckin.lifehud.service.AgentContextService;
import io.github.aomckin.lifehud.domain.DashboardHeadline;
import io.github.aomckin.lifehud.service.DashboardHeadlineService;
import io.github.aomckin.lifehud.domain.DashboardSelections;
import io.github.aomckin.lifehud.service.DashboardSelectionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public final class DashboardController {
    private final AgentContextService context;private final DashboardHeadlineService headlines;private final DashboardSelectionService selections;
    public DashboardController(AgentContextService context,DashboardHeadlineService headlines,DashboardSelectionService selections){this.context=context;this.headlines=headlines;this.selections=selections;}
    @GetMapping public DashboardSummary summary(){return context.dashboard();}
    @GetMapping("/headlines") public java.util.List<DashboardHeadline> headlines(){return headlines.all();}
    @PostMapping("/headlines") public DashboardHeadline add(@RequestBody HeadlineRequest request){return headlines.add(request==null?null:request.text());}
    @DeleteMapping("/headlines/{id}") public void remove(@PathVariable String id){headlines.remove(id);}
    @GetMapping("/selections") public DashboardSelections selections(){return selections.get();}
    @PutMapping("/selections/{slot}") public DashboardSelections select(@PathVariable String slot,@RequestBody SelectionRequest request){return selections.select(slot,request.id(),request.type());}
    public record HeadlineRequest(String text) { }
    public record SelectionRequest(String id,String type) { }
}
