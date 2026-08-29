package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.MealService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/life/meals")
public final class MealController {
    private final MealService service;
    public MealController(MealService service) { this.service = service; }

    @GetMapping public List<MealRecord> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public MealRecord create(@RequestBody MealRequest request) { return service.create(request); }
    @GetMapping("/{id}") public MealRecord get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public MealRecord update(@PathVariable String id, @RequestBody MealRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
