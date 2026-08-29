package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

public record JournalRequest(String content, Instant occurredAt, List<String> images, List<String> tags) { }
