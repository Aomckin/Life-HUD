package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import org.springframework.stereotype.Component;

/**
 * Externalized growth economy; tune it in data/growth.json instead of business code.
 * Energy is the fast, short-cycle quantity; EXP accumulates only from actually spent
 * Energy at the energy_per_exp ratio, keeping an integer remainder pool so small
 * spends are never lost.
 */
@Component
public final class GrowthEconomy {
    public static final String FILE = "growth.json";
    private final JsonFileStore files;

    public GrowthEconomy(JsonFileStore files) { this.files = files; }

    /** Effective Focus minutes per +1 Energy (default 3: 1h Focus → +20 Energy). */
    public int focusMinutesPerEnergy() { return positive("focus_minutes_per_energy", 3); }

    /** Maximum Energy a single Focus event may settle (default 80 ≈ 4h effective Focus). */
    public int focusEnergyEventCap() { return positive("focus_energy_event_cap", 80); }

    /** Actual SPEND Energy required for +1 EXP (default 10). */
    public int energyPerExp() { return positive("energy_per_exp", 10); }

    /** Safety cap for Energy earned by a single Task (default 20); task tiering itself is content. */
    public int taskEnergyCap() { return positive("task_energy_cap", 20); }

    /** Growth log entries shown in the Overview (default 12). */
    public int overviewHistoryLimit() { return positive("overview_history_limit", 12); }

    /** Trend window in days shown in the Overview (default 7). */
    public int trendDays() { return positive("trend_days", 7); }

    /** Energy earned by one Focus event, clamped by the single-event cap. */
    public int focusEnergy(long effectiveMinutes) {
        return (int) Math.min(focusEnergyEventCap(), effectiveMinutes / focusMinutesPerEnergy());
    }

    /** pool = oldRemainder + actualSpent; exp = pool / energyPerExp; remainder = pool % energyPerExp. */
    public SpendConversion convertSpentEnergy(int oldRemainder, int actualSpentEnergy) {
        int perExp = energyPerExp();
        int pool = Math.max(0, oldRemainder) + Math.max(0, actualSpentEnergy);
        return new SpendConversion(pool / perExp, pool % perExp);
    }

    public record SpendConversion(int expGained, int newRemainder) { }

    private JsonNode settings() {
        if (!files.exists(FILE)) return JsonNodeFactory.instance.objectNode();
        JsonNode node = files.read(FILE);
        if (node.hasNonNull("energy_per_exp")) return node;
        if (node.hasNonNull("exp_per_energy")) return migrateLegacyRatio(node);
        return node;
    }

    /** One-time conversion of the retired exp_per_energy field into energy_per_exp. */
    private JsonNode migrateLegacyRatio(JsonNode node) {
        double legacy = node.path("exp_per_energy").asDouble(1.0);
        int converted = legacy > 0 ? Math.max(1, (int) Math.round(1.0 / legacy)) : 10;
        if (node instanceof ObjectNode object) {
            object.remove("exp_per_energy");
            object.put("energy_per_exp", converted);
            files.write(FILE, object);
        }
        return node;
    }

    private int positive(String field, int fallback) {
        int value = settings().path(field).asInt(fallback);
        return value > 0 ? value : fallback;
    }
}
