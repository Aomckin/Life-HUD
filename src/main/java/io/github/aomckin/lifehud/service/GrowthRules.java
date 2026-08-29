package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.core.GameConfig;
import io.github.aomckin.lifehud.domain.EnergyChangeType;
import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import org.springframework.stereotype.Component;

/**
 * The single, auditable entry point for v0.4 growth arithmetic.
 * Energy is earned by real-life activity; EXP is settled by GrowthEngine only from
 * Energy actually spent (SPEND), using the ratio externalized in growth.json.
 * All wording comes from content/growth-copy.json; bounds from growth.json / config.json.
 */
@Component
public final class GrowthRules {
    private final GrowthEconomy economy;
    private final GrowthCopy copy;
    private final GameConfig config;

    public GrowthRules(GrowthEconomy economy, GrowthCopy copy, GameConfig config) {
        this.economy = economy; this.copy = copy; this.config = config;
    }

    public Change evaluate(LifeEvent event) {
        if (Boolean.TRUE.equals(event.metadata().get("test"))) return new Change(EnergyChangeType.ADJUST, 0, copy.testExcludedReason());
        return switch (event.type()) {
            case FOCUS_FINISHED -> focus(event);
            case TASK_COMPLETED -> task(event);
            case ENERGY_SPENT -> spend(event);
            case ENERGY_CHANGED -> byMetadata(event);
            default -> new Change(EnergyChangeType.ADJUST, 0, copy.lifeEventRecordedReason());
        };
    }

    /** Focus no longer yields EXP directly; only effective minutes convert into Energy at the externalized rate. */
    private Change focus(LifeEvent event) {
        long minutes = Math.max(0, number(event, "effectiveSeconds") / 60);
        return new Change(EnergyChangeType.EARN, economy.focusEnergy(minutes), copy.focusEnergyReason(minutes));
    }

    /** Tasks earn Energy only; the legacy baseExp field is deprecated and never read. */
    private Change task(LifeEvent event) {
        int energy = clamp((int) number(event, "baseEnergy"), 0, economy.taskEnergyCap());
        return new Change(EnergyChangeType.EARN, energy, copy.taskEnergyReason());
    }

    private Change spend(LifeEvent event) {
        int requested = (int) Math.max(0, number(event, "requestedEnergy"));
        // The event title names the real activity (e.g. the entertainment title) in logs and history.
        String reason = event.title() == null || event.title().isBlank() ? copy.spendReason() : event.title();
        return new Change(EnergyChangeType.SPEND, -requested, reason);
    }

    private Change byMetadata(LifeEvent event) {
        EnergyChangeType type = EnergyChangeType.from(event.metadata().get("changeType"));
        int limit = Math.max(1, config.maxEnergy());
        int delta = clamp((int) number(event, "energyDelta"), -limit, limit);
        // The event title (e.g. the day-boundary drift reason) names the change in logs and history.
        String reason = event.title() == null || event.title().isBlank() ? defaultReason(type) : event.title();
        return switch (type) {
            case EARN -> new Change(EnergyChangeType.EARN, Math.max(0, delta), reason);
            case SPEND -> new Change(EnergyChangeType.SPEND, -Math.abs(delta), reason);
            case DECAY -> new Change(EnergyChangeType.DECAY, -Math.abs(delta), reason);
            case ADJUST -> new Change(EnergyChangeType.ADJUST, delta, reason);
        };
    }

    private String defaultReason(EnergyChangeType type) {
        return switch (type) {
            case EARN -> copy.earnRecordedReason();
            case SPEND -> copy.spendReason();
            case DECAY -> copy.decayReason();
            case ADJUST -> copy.adjustReason();
        };
    }

    /** Exposed for GrowthEngine so conversion arithmetic lives only here, never in callers. */
    public GrowthEconomy.SpendConversion convertSpentEnergy(int oldRemainder, int actualSpentEnergy) {
        return economy.convertSpentEnergy(oldRemainder, actualSpentEnergy);
    }

    private long number(LifeEvent event, String key) {
        Object value = event.metadata().get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public record Change(EnergyChangeType type, int energy, String reason) { }
}
