package io.github.aomckin.lifehud.dto;

import java.time.Instant;
import java.util.List;

public record FocusManualRequest(String title, Instant startedAt, Instant endedAt, String note,
                                 List<String> relatedTaskIds) { }
