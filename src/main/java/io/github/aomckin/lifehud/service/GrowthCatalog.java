package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.GrowthAchievementCondition;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Content-backed growth catalog loaded from data/content/growth-achievements.json and
 * growth-titles.json. Achievements, titles and their copy are content, not code: add
 * entries there without recompiling. Missing content files fail startup fast.
 */
@Component
public final class GrowthCatalog {
    public static final String ACHIEVEMENTS_FILE = "content/growth-achievements.json";
    public static final String TITLES_FILE = "content/growth-titles.json";
    private static final TypeReference<List<AchievementRule>> ACHIEVEMENTS_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<TitleDefinition>> TITLES_TYPE = new TypeReference<>() { };

    private final List<AchievementRule> achievements;
    private final List<TitleDefinition> titles;

    public GrowthCatalog(JsonFileStore files, ObjectMapper mapper) {
        this.achievements = load(files, mapper, ACHIEVEMENTS_FILE, ACHIEVEMENTS_TYPE);
        this.titles = load(files, mapper, TITLES_FILE, TITLES_TYPE);
    }

    public List<AchievementRule> achievements() { return achievements; }
    public List<TitleDefinition> titles() { return titles; }
    public AchievementRule achievement(String id) {
        return achievements.stream().filter(v -> v.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("内容目录缺少成就: " + id));
    }
    public TitleDefinition title(String id) {
        return titles.stream().filter(v -> v.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("内容目录缺少称号: " + id));
    }

    private <T> List<T> load(JsonFileStore files, ObjectMapper mapper, String file, TypeReference<List<T>> type) {
        if (!files.exists(file)) throw new IllegalStateException("缺少内容文件: " + file + "（请恢复 data/content/ 目录）");
        try { return mapper.convertValue(files.read(file), type); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("无法读取内容文件: " + file, e); }
    }

    public record AchievementRule(String id, String name, String description, String category, boolean hidden,
                                  String icon, GrowthAchievementCondition condition, String relatedTitleId) { }
    public record TitleDefinition(String id, String name, String description, String sourceType, String sourceId,
                                  boolean hidden, boolean custom) { }
}
