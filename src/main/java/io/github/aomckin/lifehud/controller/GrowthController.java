package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.service.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/growth")
public final class GrowthController {
    private final GrowthService growth;private final GrowthSnapshotService snapshots;private final EnergyLedgerService ledger;
    public GrowthController(GrowthService growth,GrowthSnapshotService snapshots,EnergyLedgerService ledger){this.growth=growth;this.snapshots=snapshots;this.ledger=ledger;}
    @GetMapping public Map<String,Object> get(){return growth.overview();}
    @GetMapping("/history") public List<GrowthEventRecord> history(@RequestParam(defaultValue="30")int limit){return growth.history(limit);}
    @GetMapping("/energy-history") public List<EnergyRecord> energy(){return growth.energyHistory();}
    @GetMapping("/snapshots") public List<GrowthSnapshot> snapshots(@RequestParam(defaultValue="30")int days){return snapshots.recent(days);}
    @PostMapping("/recalculate") public Map<String,Object> recalculate(){return Map.of("unlocked",growth.recalculate(),"growth",growth.overview());}
    @PostMapping("/energy/spend") public Map<String,Object> spend(@RequestBody EnergySpendRequest request){return ledger.spend(request);}
    @PostMapping("/energy/adjust") public Map<String,Object> adjust(@RequestBody EnergyAdjustRequest request){return ledger.adjust(request);}
}
