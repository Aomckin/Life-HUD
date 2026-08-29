package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.Dream;
import io.github.aomckin.lifehud.domain.DreamMilestone;
import io.github.aomckin.lifehud.domain.DreamMilestoneRequest;
import io.github.aomckin.lifehud.domain.DreamRequest;
import io.github.aomckin.lifehud.domain.Goal;
import io.github.aomckin.lifehud.domain.GoalRequest;
import io.github.aomckin.lifehud.service.DreamService;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public final class DreamController {
    private final DreamService dreams;
    public DreamController(DreamService dreams){this.dreams=dreams;}

    @GetMapping("/api/dreams") public List<Dream> all(){return dreams.all();}
    @PostMapping("/api/dreams") @ResponseStatus(HttpStatus.CREATED) public Dream create(@RequestBody DreamRequest request){return dreams.create(request);}
    @GetMapping("/api/dreams/{id}") public Map<String,Object> detail(@PathVariable String id){
        Map<String,Object> map=new LinkedHashMap<>();
        map.put("dream",dreams.get(id));
        List<Map<String,Object>> goals=new ArrayList<>();
        dreams.goalsOf(id).forEach(goal->{
            Map<String,Object> view=new LinkedHashMap<>();
            view.put("goal",goal);
            view.put("milestones",dreams.milestonesOf(goal.id()));
            goals.add(view);
        });
        map.put("goals",goals);
        return map;
    }
    @PutMapping("/api/dreams/{id}") public Dream update(@PathVariable String id,@RequestBody DreamRequest request){return dreams.update(id,request);}
    @DeleteMapping("/api/dreams/{id}") public Dream archive(@PathVariable String id){return dreams.archive(id);}
    @PostMapping("/api/dreams/{id}/complete") public Dream complete(@PathVariable String id){return dreams.complete(id);}
    @PostMapping("/api/dreams/{id}/pause") public Dream pause(@PathVariable String id){return dreams.pause(id);}
    @PostMapping("/api/dreams/{id}/resume") public Dream resume(@PathVariable String id){return dreams.resume(id);}

    @PostMapping("/api/dreams/{dreamId}/goals") @ResponseStatus(HttpStatus.CREATED)
    public Goal createGoal(@PathVariable String dreamId,@RequestBody GoalRequest request){return dreams.createGoal(dreamId,request);}
    @PutMapping("/api/goals/{id}") public Goal updateGoal(@PathVariable String id,@RequestBody GoalRequest request){return dreams.updateGoal(id,request);}
    @PostMapping("/api/goals/{id}/complete") public Goal completeGoal(@PathVariable String id){return dreams.completeGoal(id);}
    @DeleteMapping("/api/goals/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteGoal(@PathVariable String id){dreams.deleteGoal(id);}

    @PostMapping("/api/goals/{goalId}/milestones") @ResponseStatus(HttpStatus.CREATED)
    public DreamMilestone createMilestone(@PathVariable String goalId,@RequestBody DreamMilestoneRequest request){return dreams.createMilestone(goalId,request);}
    @PutMapping("/api/dream-milestones/{id}") public DreamMilestone updateMilestone(@PathVariable String id,@RequestBody DreamMilestoneRequest request){return dreams.updateMilestone(id,request);}
    @PostMapping("/api/dream-milestones/{id}/complete") public DreamMilestone completeMilestone(@PathVariable String id){return dreams.completeMilestone(id);}
    @DeleteMapping("/api/dream-milestones/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteMilestone(@PathVariable String id){dreams.deleteMilestone(id);}
}
