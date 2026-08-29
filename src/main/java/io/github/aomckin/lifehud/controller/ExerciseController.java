package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.ExerciseService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/life/exercises")
public final class ExerciseController {
    private final ExerciseService service;
    public ExerciseController(ExerciseService service) { this.service = service; }

    @GetMapping public List<ExerciseRecord> all() { return service.all(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ExerciseRecord create(@RequestBody ExerciseRequest request) { return service.create(request); }
    @GetMapping("/{id}") public ExerciseRecord get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public ExerciseRecord update(@PathVariable String id, @RequestBody ExerciseRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id) { service.remove(id); }
}
