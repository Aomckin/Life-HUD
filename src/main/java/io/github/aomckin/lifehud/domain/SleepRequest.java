package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** Create/update payload for SleepRecord; null fields keep their value on update. */
public record SleepRequest(Instant sleepTime, Instant wakeTime, Integer quality, String type, String note) { }
