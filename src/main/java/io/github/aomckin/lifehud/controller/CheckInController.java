package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.CheckInService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/life/check-ins")
public final class CheckInController {
    private final CheckInService service;
    public CheckInController(CheckInService service) { this.service = service; }

    @GetMapping public List<CheckIn> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CheckIn create(@RequestBody CheckInRequest request) { return service.create(request); }
    @GetMapping("/{id}") public CheckIn get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public CheckIn update(@PathVariable String id, @RequestBody CheckInRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
