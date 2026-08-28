package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.dto.FocusFinishRequest;
import io.github.aomckin.lifehud.dto.FocusSessionView;
import io.github.aomckin.lifehud.dto.FocusStartRequest;
import io.github.aomckin.lifehud.dto.FocusTodayView;
import io.github.aomckin.lifehud.dto.FocusSegmentRequest;
import io.github.aomckin.lifehud.dto.FocusManualRequest;
import io.github.aomckin.lifehud.service.FocusService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/focus")
public final class FocusController {
    private final FocusService focus;
    public FocusController(FocusService focus) { this.focus = focus; }

    @PostMapping("/start") public FocusSessionView start(@RequestBody FocusStartRequest request) { return focus.start(request); }
    @PostMapping("/{id}/pause") public FocusSessionView pause(@PathVariable String id) { return focus.pause(id); }
    @PostMapping("/{id}/resume") public FocusSessionView resume(@PathVariable String id) { return focus.resume(id); }
    @PostMapping("/{id}/segments/switch") public FocusSessionView switchSegment(@PathVariable String id,
            @RequestBody FocusSegmentRequest request) { return focus.switchSegment(id, request); }
    @PatchMapping("/{id}/segments/{segmentId}") public FocusSessionView updateSegment(@PathVariable String id,
            @PathVariable String segmentId, @RequestBody FocusSegmentRequest request) {
        return focus.updateSegment(id, segmentId, request);
    }
    @PostMapping("/manual") public FocusSessionView manual(@RequestBody FocusManualRequest request) {
        return focus.manual(request);
    }
    @PostMapping("/{id}/complete") public FocusSessionView complete(@PathVariable String id,
            @RequestBody(required = false) FocusFinishRequest request) {
        return focus.complete(id, request == null ? null : request.note());
    }
    @PostMapping("/{id}/interrupt") public FocusSessionView interrupt(@PathVariable String id,
            @RequestBody(required = false) FocusFinishRequest request) {
        return focus.interrupt(id, request == null ? null : request.note());
    }
    @GetMapping("/current") public FocusSessionView current() { return focus.current(); }
    @GetMapping("/today") public FocusTodayView today() { return focus.today(); }
    @GetMapping("/history") public List<FocusSessionView> history(@RequestParam(defaultValue = "30") int limit) {
        return focus.history(limit);
    }
}
