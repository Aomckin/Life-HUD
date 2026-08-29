package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    private static Path resolveRoot(String propertyRoot) {
        String configured = propertyRoot == null || propertyRoot.isBlank()
                ? Optional.ofNullable(System.getenv("LIFEHUD_DATA_DIR")).orElse(System.getenv("OTAKU_ENERGY_DATA_DIR"))
                : propertyRoot;
        return Path.of(configured == null || configured.isBlank() ? "data" : configured)
                .toAbsolutePath().normalize();
    }
}
