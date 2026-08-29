package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.Player;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import io.github.aomckin.lifehud.repository.PlayerRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Day-boundary Energy drift: on a new day Energy regresses toward the midpoint —
 * the surplus or deficit above/below it is multiplied by the content-tunable factor.
 * Values live in data/content/energy-drift.json; the change is settled through the
 * ordinary LifeEvent → GrowthEngine path as an ADJUST (never yields EXP).
 */
@Service
public final class EnergyDriftService {
    public static final String FILE = "content/energy-drift.json";
    private final Player player;
    private final PlayerRepository players;
    private final LifeEventService events;
    private final GrowthCopy copy;
    private final JsonFileStore files;

    public EnergyDriftService(Player player, PlayerRepository players, LifeEventService events,
                              GrowthCopy copy, JsonFileStore files) {
        this.player = player; this.players = players; this.events = events; this.copy = copy; this.files = files;
    }

    /** Baseline Energy the day-start drift regresses toward (default 90). */
    public int midpoint() { return Math.max(0, settings().path("midpoint").asInt(90)); }

    /** Multiplier applied to the part above/below the midpoint (default 0.75). */
    public double dayStartFactor() {
        double factor = settings().path("day_start_factor").asDouble(0.75);
        return Math.max(0, Math.min(1, factor));
    }

    @PostConstruct
    public synchronized void applyIfNeeded() {
        LocalDate today = LocalDate.now();
        if (today.toString().equals(player.energy_drift_date)) return;
        player.energy_drift_date = today.toString();
        players.save(player);
        int midpoint = midpoint();
        int target = midpoint + (int) Math.round((player.energy - midpoint) * dayStartFactor());
        int delta = target - player.energy;
        if (delta == 0) return;
        String reason = copy.energyDriftReason(midpoint);
        events.record(LifeEventType.ENERGY_CHANGED, "system", reason, reason, List.of("growth", "energy"),
                Map.of("changeType", "ADJUST", "energyDelta", delta, "sourceId", "energy-drift"));
    }

    private com.fasterxml.jackson.databind.JsonNode settings() {
        return files.exists(FILE) ? files.read(FILE) : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    }
}
