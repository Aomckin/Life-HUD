package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.repository.JsonFileStore;
import org.springframework.stereotype.Component;

/** App-level content (name/version) from data/content/app.json, instead of a hardcoded string. */
@Component
public final class AppInfo {
    public static final String FILE = "content/app.json";
    private final JsonFileStore files;

    public AppInfo(JsonFileStore files) { this.files = files; }

    public String name() { return files.exists(FILE) ? files.read(FILE).path("name").asText("Life HUD") : "Life HUD"; }
    public String version() { return files.exists(FILE) ? files.read(FILE).path("version").asText("dev") : "dev"; }
}
