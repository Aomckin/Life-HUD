package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.aomckin.lifehud.domain.AchievementConditionType;
import io.github.aomckin.lifehud.domain.AchievementDefinition;
import io.github.aomckin.lifehud.domain.TaskRequirement;
import io.github.aomckin.lifehud.domain.TaskSource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Maps achievements.json into the domain model. JSON details do not escape this boundary. */
@Repository
public final class AchievementRepository {
    private final JsonFileStore files;

    public AchievementRepository(JsonFileStore files) {
        this.files = files;
    }

    public List<AchievementDefinition> findAll() {
        List<AchievementDefinition> definitions = new ArrayList<>();
        for (JsonNode node : files.read("achievements.json")) {
            definitions.add(toDefinition(node));
        }
        return List.copyOf(definitions);
    }

    private AchievementDefinition toDefinition(JsonNode node) {
        return new AchievementDefinition(
                node.path("id").asText(),
                node.path("name").asText(),
                node.path("desc").asText(),
                AchievementConditionType.fromJson(node.path("condition_type").asText()),
                node.path("target_value").asInt(),
                node.path("target_action").asText(),
                requirements(node.path("requirements")),
                node.path("reward_coin").asInt(),
                node.path("reward_exp").asInt());
    }

    private List<TaskRequirement> requirements(JsonNode nodes) {
        List<TaskRequirement> requirements = new ArrayList<>();
        for (JsonNode node : nodes) {
            requirements.add(new TaskRequirement(
                    TaskSource.fromJson(node.path("source").asText()),
                    node.path("task_id").asText(),
                    node.path("count").asInt(1)));
        }
        return requirements;
    }
}