package io.github.aomckin.lifehud.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.aomckin.lifehud.repository.JsonFileStore;
import org.springframework.stereotype.Component;

/**
 * Growth UI/log copy loaded from data/content/growth-copy.json. Reasons, derived-event
 * wording, legacy fallbacks and command messages are content, not code.
 */
@Component
public final class GrowthCopy {
    public static final String FILE = "content/growth-copy.json";
    private final JsonFileStore files;

    public GrowthCopy(JsonFileStore files) { this.files = files; }

    public String focusEnergyReason(long minutes) { return format("reason.focusEnergy", "minutes", minutes); }
    public String taskEnergyReason() { return text("reason.task"); }
    public String spendReason() { return text("reason.spend"); }
    public String earnRecordedReason() { return text("reason.earnRecorded"); }
    public String decayReason() { return text("reason.decay"); }
    public String adjustReason() { return text("reason.adjust"); }
    public String testExcludedReason() { return text("reason.testExcluded"); }
    public String lifeEventRecordedReason() { return text("reason.lifeEventRecorded"); }

    public String levelUpTitle() { return text("derived.levelUpTitle"); }
    public String levelUpDescription(int before, int after) {
        return format("derived.levelUpDescription", "before", before).replace("{after}", String.valueOf(after));
    }
    public String growthTag() { return text("derived.growthTag"); }
    public String achievementTag() { return text("derived.achievementTag"); }
    public String titleTag() { return text("derived.titleTag"); }
    public String milestoneTag() { return text("derived.milestoneTag"); }

    public String legacyAchievementDescription() { return text("legacy.achievementDescription"); }
    public String legacyTitleDescription() { return text("legacy.titleDescription"); }
    public String hiddenAchievementName() { return text("legacy.hiddenAchievementName"); }
    public String hiddenAchievementDescription() { return text("legacy.hiddenAchievementDescription"); }

    public String noTitleSelected() { return text("title.noSelection"); }
    public String titleEquippedDescription() { return text("title.equippedDescription"); }
    public String titleUnequippedTitle() { return text("title.unequippedTitle"); }
    public String titleUnequippedDescription() { return text("title.unequippedDescription"); }

    public String milestoneDefaultCategory() { return text("milestone.defaultCategory"); }

    public String entertainmentRecordedDescription() { return text("entertainment.recordedDescription"); }
    public String entertainmentUpdatedDescription() { return text("entertainment.updatedDescription"); }
    public String entertainmentDeletedDescription() { return text("entertainment.deletedDescription"); }

    public String energyDriftReason(int midpoint) { return format("drift.reason", "midpoint", midpoint); }

    public String shopDisabledMessage() { return text("command.shopDisabled"); }
    public String actionDisabledMessage() { return text("command.actionDisabled"); }

    public String taskCompletedLog(String name) { return format("task.logCompleted", "name", name); }
    public String specialTaskCompletedLog(String name) { return format("task.logSpecialCompleted", "name", name); }
    public String growthSyncedLog() { return text("task.logGrowthSynced"); }

    private String text(String path) { return at(path).asText(""); }
    private String format(String path, String key, Object value) {
        String template = text(path);
        return key == null ? template : template.replace("{" + key + "}", String.valueOf(value));
    }
    private JsonNode node() { return files.read(FILE); }
    /** JsonNode.path() does not understand dotted paths; navigate one segment at a time. */
    private JsonNode at(String path) {
        JsonNode node = node();
        for (String segment : path.split("\\.")) node = node.path(segment);
        return node;
    }
}
