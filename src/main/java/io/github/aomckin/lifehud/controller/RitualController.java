package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.Ritual;
import io.github.aomckin.lifehud.domain.RitualExecution;
import io.github.aomckin.lifehud.domain.RitualRequest;
import io.github.aomckin.lifehud.service.RitualService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public final class RitualController {
    private final RitualService rituals;
    public RitualController(RitualService rituals){this.rituals=rituals;}

    @GetMapping("/api/rituals") public List<Ritual> all(){return rituals.all();}
    @PostMapping("/api/rituals") @ResponseStatus(HttpStatus.CREATED) public Ritual create(@RequestBody RitualRequest request){return rituals.create(request);}
    @GetMapping("/api/rituals/{id}") public Map<String,Object> detail(@PathVariable String id){return rituals.detail(id);}
    @PutMapping("/api/rituals/{id}") public Ritual update(@PathVariable String id,@RequestBody RitualRequest request){return rituals.update(id,request);}
    @DeleteMapping("/api/rituals/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){rituals.delete(id);}
    @PostMapping("/api/rituals/{id}/enable") public Ritual enable(@PathVariable String id){return rituals.setEnabled(id,true);}
    @PostMapping("/api/rituals/{id}/disable") public Ritual disable(@PathVariable String id){return rituals.setEnabled(id,false);}

    @PostMapping("/api/rituals/{id}/start") public RitualExecution start(@PathVariable String id){return rituals.start(id);}
    @GetMapping("/api/ritual-executions/{id}") public RitualService.ExecutionView execution(@PathVariable String id){return rituals.executionView(id);}
    @PostMapping("/api/ritual-executions/{id}/steps/{stepId}") public RitualExecution updateStep(@PathVariable String id,@PathVariable String stepId,
            @RequestBody Map<String,Object> body){
        return rituals.updateStep(id,stepId,Boolean.TRUE.equals(body.get("done")),(String)body.get("note"));
    }
    @PostMapping("/api/ritual-executions/{id}/complete") public RitualExecution complete(@PathVariable String id,
            @RequestBody(required=false) Map<String,String> body){return rituals.complete(id,body==null?"":body.getOrDefault("note",""));}
    @PostMapping("/api/ritual-executions/{id}/cancel") public RitualExecution cancel(@PathVariable String id,
            @RequestBody(required=false) Map<String,String> body){return rituals.cancel(id,body==null?"":body.getOrDefault("note",""));}
}
