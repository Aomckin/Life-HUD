package io.github.aomckin.lifehud.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

/** One date boundary for Dashboard, Agent Context and future reports. */
@Service
public final class LifeDateService {
    private final Clock clock;

    public LifeDateService() { this(Clock.systemDefaultZone()); }
    LifeDateService(Clock clock) { this.clock = clock; }

    public Instant now() { return clock.instant(); }
    public ZoneId zone() { return clock.getZone(); }
    public LocalDate today() { return LocalDate.now(clock); }
    public Instant startOf(LocalDate date) { return date.atStartOfDay(zone()).toInstant(); }
    public Instant endExclusive(LocalDate date) { return startOf(date.plusDays(1)); }
    public boolean isOn(Instant instant, LocalDate date) {
        return instant != null && !instant.isBefore(startOf(date)) && instant.isBefore(endExclusive(date));
    }
}
