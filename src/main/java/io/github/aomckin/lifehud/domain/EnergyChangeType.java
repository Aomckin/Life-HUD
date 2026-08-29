package io.github.aomckin.lifehud.domain;

/** Lifecycle classification of an Energy change; only SPEND converts into EXP. */
public enum EnergyChangeType {
    EARN, SPEND, DECAY, ADJUST;

    /** Lenient parsing for event metadata; unknown or missing values fall back to ADJUST. */
    public static EnergyChangeType from(Object value) {
        if (value instanceof EnergyChangeType type) return type;
        if (value != null) {
            try { return EnergyChangeType.valueOf(String.valueOf(value).trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        return ADJUST;
    }
}
