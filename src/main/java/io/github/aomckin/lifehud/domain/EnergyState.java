package io.github.aomckin.lifehud.domain;

/** Current recoverable state; EnergyRecord remains the append-only change history. */
public record EnergyState(int current, int minimum, int maximum) {
    public EnergyState {
        if (minimum > maximum) throw new IllegalArgumentException("minimum must not exceed maximum");
        current = Math.max(minimum, Math.min(maximum, current));
    }

    public int available() { return current - minimum; }
    public int capacity() { return maximum - minimum; }
}
