package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/growth")
public final class GrowthController {
    private final GrowthService growth;private final GrowthSnapshotService snapshots;
    public GrowthController(GrowthService growth,GrowthSnapshotService snapshots){this.growth=growth;this.snapshots=snapshots;}
    @GetMapping public Map<String,Object> get(){return growth.overview();}
    @GetMapping("/history") public List<GrowthEventRecord> history(@RequestParam(defaultValue="30")int limit){return growth.history(limit);}
    @GetMapping("/energy-history") public List<EnergyRecord> energy(){return growth.energyHistory();}
    @GetMapping("/snapshots") public List<GrowthSnapshot> snapshots(@RequestParam(defaultValue="30")int days){return snapshots.recent(days);}
    @PostMapping("/recalculate") public Map<String,Object> recalculate(){return Map.of("unlocked",growth.recalculate(),"growth",growth.overview());}
}
