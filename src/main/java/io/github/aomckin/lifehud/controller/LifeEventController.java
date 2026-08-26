package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.service.LifeEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read API for the dashboard and future timeline views. */
@RestController
@RequestMapping("/api/life-events")
public final class LifeEventController {
    private final LifeEventService events;
    public LifeEventController(LifeEventService events) { this.events = events; }
    @GetMapping public List<LifeEvent> list(@RequestParam(defaultValue = "8") int limit) { return events.recent(limit); }
    @GetMapping("/recent") public List<LifeEvent> recent(@RequestParam(defaultValue = "8") int limit) { return events.recent(limit); }
}
