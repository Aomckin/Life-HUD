package io.github.aomckin.lifehud.dto;

import java.util.Map;

public record GameEvent(String type, Map<String,Object> payload) {}
