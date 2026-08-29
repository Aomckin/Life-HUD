package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.service.GrowthAchievementService;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/achievements")
public final class AchievementController {
    private final GrowthAchievementService achievements;public AchievementController(GrowthAchievementService achievements){this.achievements=achievements;}
    @GetMapping public List<Map<String,Object>> all(){return achievements.all();}
    @GetMapping("/{id}") public Map<String,Object> one(@PathVariable String id){Map<String,Object> value=achievements.find(id);if(value==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"成就不存在");return value;}
}
