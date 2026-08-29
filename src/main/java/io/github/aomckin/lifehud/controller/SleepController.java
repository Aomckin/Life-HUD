package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.SleepService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/life/sleep")
public final class SleepController {
    private final SleepService service;
    public SleepController(SleepService service) { this.service = service; }

    @GetMapping public List<SleepRecord> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SleepRecord create(@RequestBody SleepRequest request) { return service.create(request); }
    @GetMapping("/{id}") public SleepRecord get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public SleepRecord update(@PathVariable String id, @RequestBody SleepRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
