package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Application data-directory bootstrap. Generic file operations live in JsonFileStore. */
@Repository
public class JsonRepository extends JsonFileStore {
    private static final List<String> DEFAULTS = List.of(
            "config.json", "actions.json", "level.json", "tasks.json", "special_tasks.json",
            "shop.json", "achievements.json", "titles.json", "save.json", "growth.json");
    /** Editable content files seeded once into data/content/; user-tunable without recompiling. */
    private static final List<String> CONTENT_DEFAULTS = List.of(
            "content/growth-achievements.json", "content/growth-titles.json",
            "content/growth-copy.json", "content/app.json", "content/energy-drift.json");

    public JsonRepository(ObjectMapper mapper, @Value("${lifehud.data-dir:}") String propertyRoot) {
        super(mapper, resolveRoot(propertyRoot));
    }

    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(root());
        seedDefaults(DEFAULTS);
        seedDefaults(CONTENT_DEFAULTS);
        mergeTaskCatalog("tasks.json", false);
        mergeTaskCatalog("special_tasks.json", true);
    }

    private void seedDefaults(List<String> names) throws IOException {
        for (String name : names) {
            Path target = resolve(name);
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                try (var input = new ClassPathResource("data/" + name).getInputStream()) {
                    Files.copy(input, target);
                }
            }
        }
    }

    /**
     * Adds newly bundled task definitions without replacing user-owned definitions or their history.
     * Legacy special-task rewards are converted before the new EXP-native definitions are appended.
     */
    private void mergeTaskCatalog(String name, boolean special) throws IOException {
        ObjectNode stored = (ObjectNode) read(name);
        ArrayNode current = stored.withArray("tasks");
        boolean changed = false;
        if (special && !"exp-v1".equals(stored.path("reward_schema").asText())) {
            for (var node : current) if (node instanceof ObjectNode task)
                task.put("exp", Math.max(0, task.path("exp").asInt() / 4));
            stored.put("reward_schema", "exp-v1");
            changed = true;
        }

        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (var node : current) if (node instanceof ObjectNode task) {
            ids.add(task.path("id").asText());
            names.add(task.path("name").asText().trim());
            if (!task.has("note")) { task.put("note", ""); changed = true; }
        }
        ObjectNode bundled;
        try (var input = new ClassPathResource("data/" + name).getInputStream()) {
            bundled = (ObjectNode) mapper().readTree(input);
        }
        for (var node : bundled.withArray("tasks")) {
            String id = node.path("id").asText(), taskName = node.path("name").asText().trim();
            if (ids.contains(id) || names.contains(taskName)) continue;
            current.add(node.deepCopy());
            ids.add(id); names.add(taskName); changed = true;
        }
        if (changed) write(name, stored);
    }

    private static Path resolveRoot(String propertyRoot) {
        String configured = propertyRoot == null || propertyRoot.isBlank()
                ? Optional.ofNullable(System.getenv("LIFEHUD_DATA_DIR")).orElse(System.getenv("OTAKU_ENERGY_DATA_DIR"))
                : propertyRoot;
        return Path.of(configured == null || configured.isBlank() ? "data" : configured)
                .toAbsolutePath().normalize();
    }
}
