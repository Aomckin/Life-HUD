package io.github.aomckin.lifehud.dto;

import java.util.List;
import java.util.Map;

public record OperationResult(boolean success, String message, List<GameEvent> events, Map<String,Object> state) {}
