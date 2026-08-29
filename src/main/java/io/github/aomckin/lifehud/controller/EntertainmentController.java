package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.EntertainmentRecord;
import io.github.aomckin.lifehud.domain.EntertainmentRequest;
import io.github.aomckin.lifehud.service.EntertainmentRecordService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/entertainment")
public final class EntertainmentController {
    private final EntertainmentRecordService entertainment;
    public EntertainmentController(EntertainmentRecordService entertainment){this.entertainment=entertainment;}
    @GetMapping public List<EntertainmentRecord> all(){return entertainment.all();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public EntertainmentRecord create(@RequestBody EntertainmentRequest request){return entertainment.create(request);}
    @PatchMapping("/{id}") public EntertainmentRecord update(@PathVariable String id,@RequestBody EntertainmentRequest request){return entertainment.update(id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){entertainment.delete(id);}
}
