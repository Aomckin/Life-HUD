package io.github.aomckin.lifehud.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.domain.ActionDurationOption;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.util.List;

/** Read-only access to action definitions and server-controlled duration options. */
public final class ActionCatalog {
    public static final List<ActionDurationOption> POSITIVE_OPTIONS = List.of(
            new ActionDurationOption(25, 1, 0), new ActionDurationOption(45, 1.5, 0),
            new ActionDurationOption(60, 2, 0));
    public static final List<ActionDurationOption> NEGATIVE_OPTIONS = List.of(
            new ActionDurationOption(30, 1, 1), new ActionDurationOption(60, 1.5, 2),
            new ActionDurationOption(90, 2, 3));

    private final ObjectNode actions;

    public ActionCatalog(JsonFileStore files) { actions = (ObjectNode) files.read("actions.json"); }
    public boolean contains(String name) { return actions.has(name); }
    public JsonNode get(String name) { return actions.get(name); }
    public ObjectNode data() { return actions; }
    public List<ActionDurationOption> durationOptions(JsonNode action) {
        return action.path("energy_change").asInt() >= 0 ? POSITIVE_OPTIONS : NEGATIVE_OPTIONS;
    }
    public ActionDurationOption allowedOption(String name, ActionDurationOption requested) {
        if (requested == null || !contains(name)) return null;
        return durationOptions(get(name)).stream().filter(o -> o.minutes() == requested.minutes()).findFirst().orElse(null);
    }
}
