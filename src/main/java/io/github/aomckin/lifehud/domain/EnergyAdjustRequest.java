package io.github.aomckin.lifehud.domain;

/** Request body for manual Energy corrections; ADJUST never yields EXP. */
public record EnergyAdjustRequest(int delta, String reason) { }
