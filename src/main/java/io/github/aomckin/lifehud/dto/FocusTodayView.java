package io.github.aomckin.lifehud.dto;

import java.util.Map;

public record FocusTodayView(long totalSeconds, long totalMinutes, long actualSeconds, int sessionCount,
                             long longestSeconds, Map<String, Long> modeSeconds) { }
