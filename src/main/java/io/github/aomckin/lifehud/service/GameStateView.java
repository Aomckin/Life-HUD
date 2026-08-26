package io.github.aomckin.lifehud.service;

import java.util.Map;

/** Stable front-end-facing state payload returned by GET /state. */
public record GameStateView(Map<String, Object> data) {
    public Map<String, Object> asMap() { return data; }
}
