package io.github.aomckin.lifehud.domain;

import java.time.Instant;

/** One user-authored sentence that may become today's Dashboard title. */
public record DashboardHeadline(String id, String text, Instant createdAt) { }
