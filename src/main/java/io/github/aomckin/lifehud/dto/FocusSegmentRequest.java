package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.domain.FocusSegmentType;

public record FocusSegmentRequest(FocusSegmentType type, String title, String relatedTaskId, String note) { }
