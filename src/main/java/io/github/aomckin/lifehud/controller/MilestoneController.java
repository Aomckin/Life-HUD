package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.MilestoneService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/milestones")
public final class MilestoneController {
    private final MilestoneService milestones;public MilestoneController(MilestoneService milestones){this.milestones=milestones;}
    @GetMapping public List<Milestone> all(){return milestones.all();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Milestone create(@RequestBody MilestoneRequest request){return milestones.create(request);}
    @PatchMapping("/{id}") public Milestone update(@PathVariable String id,@RequestBody MilestoneRequest request){return milestones.update(id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){milestones.delete(id);}
}
