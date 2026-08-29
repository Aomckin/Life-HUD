package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.domain.TimelineItem;
import io.github.aomckin.lifehud.service.TimelineService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** One question, one answer: what happened, when, in which slice of life. */
@RestController @RequestMapping("/api/timeline")
public final class TimelineController {
    private final TimelineService timeline;
    public TimelineController(TimelineService timeline) { this.timeline = timeline; }

    @GetMapping
    public List<TimelineItem> timeline(@RequestParam(required = false) String date,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       @RequestParam(required = false) String sources,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return timeline.timeline(date, startDate, endDate, sources, type, page, size);
    }
}
