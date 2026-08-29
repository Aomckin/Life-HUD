package io.github.aomckin.lifehud.domain;

/** Request body for spending Energy through the unified ledger. */
public record EnergySpendRequest(int amount, String reason, String sourceType, String sourceId) { }
