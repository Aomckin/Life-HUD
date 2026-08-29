package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.TitleRequest;
import io.github.aomckin.lifehud.service.GrowthTitleService;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/titles")
public final class TitleController {
    private final GrowthTitleService titles;public TitleController(GrowthTitleService titles){this.titles=titles;}
    @GetMapping public List<Map<String,Object>> all(){return titles.all();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody TitleRequest request){return titles.create(request);}
    @PostMapping("/{id}/equip") public Map<String,Object> equip(@PathVariable String id){return titles.equip(id);}
    @PostMapping("/unequip") public Map<String,Object> unequip(){titles.unequip();return Map.of("equipped",false);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){titles.delete(id);}
}
