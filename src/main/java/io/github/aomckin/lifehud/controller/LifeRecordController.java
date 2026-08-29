package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.LifeRecordService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/life/records")
public final class LifeRecordController {
    private final LifeRecordService service;
    public LifeRecordController(LifeRecordService service) { this.service = service; }

    @GetMapping public List<LifeRecord> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public LifeRecord create(@RequestBody LifeRecordRequest request) { return service.create(request); }
    @GetMapping("/{id}") public LifeRecord get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public LifeRecord update(@PathVariable String id, @RequestBody LifeRecordRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
