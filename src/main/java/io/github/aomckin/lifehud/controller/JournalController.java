package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.JournalService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/journal")
public final class JournalController {
    private final JournalService service;
    public JournalController(JournalService service) { this.service = service; }

    @GetMapping public List<JournalEntry> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public JournalEntry create(@RequestBody JournalRequest request) { return service.create(request); }
    @GetMapping("/{id}") public JournalEntry get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public JournalEntry update(@PathVariable String id, @RequestBody JournalRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
