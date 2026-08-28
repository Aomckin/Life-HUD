package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.domain.FocusMode;

public record FocusStartRequest(FocusMode mode, String title, String taskId, Integer plannedMinutes,
                                Integer breakMinutes, java.util.List<String> relatedTaskIds) {
    public FocusStartRequest(FocusMode mode, String title, String taskId, Integer plannedMinutes) {
        this(mode, title, taskId, plannedMinutes, null, null);
    }
}
