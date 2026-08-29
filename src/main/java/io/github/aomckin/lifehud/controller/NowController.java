package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.NowSnapshot;
import io.github.aomckin.lifehud.domain.NowState;
import io.github.aomckin.lifehud.service.NowService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/now")
public final class NowController {
    private final NowService now;
    public NowController(NowService now){this.now=now;}

    @GetMapping public NowState current(){return now.current();}
    @PutMapping public NowState update(@RequestBody NowState state){return now.update(state);}
    @PostMapping(value="/songs",consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public NowState uploadSong(@RequestParam("file") MultipartFile file,@RequestParam("slot") int slot){return now.uploadSong(slot,file);}
    @DeleteMapping("/songs/{slot}") public NowState removeSong(@PathVariable int slot){return now.removeSong(slot);}
    @PostMapping("/snapshots") @ResponseStatus(HttpStatus.CREATED) public NowSnapshot createSnapshot(){return now.createSnapshot();}
    @GetMapping("/snapshots") public List<NowSnapshot> snapshots(){return now.snapshots();}
    @GetMapping("/snapshots/{id}") public NowSnapshot snapshot(@PathVariable String id){return now.snapshot(id);}
    @DeleteMapping("/snapshots/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSnapshot(@PathVariable String id){now.deleteSnapshot(id);}
}
