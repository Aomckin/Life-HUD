package io.github.aomckin.lifehud.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Typed access to config.json without exposing configuration lookup to business services. */
public final class GameConfig {
    private final ObjectNode data;
    public GameConfig(ObjectNode data) {
        this.data = data;
        if (!data.has("theme")) data.put("theme", "default");
    }
    public ObjectNode data() { return data; }
    public int defaultEnergy() { return data.path("default_energy").asInt(); }
    public int maxEnergy() { return data.path("max_energy").asInt(); }
    public String windowTitle() { return data.path("window_title").asText(); }
    public String windowSize() { return data.path("window_size").asText(); }
}
