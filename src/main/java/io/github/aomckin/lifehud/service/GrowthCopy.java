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

    public String dreamCreatedDescription(String title) { return format("direction.dreamCreated", "title", title); }
    public String dreamCompletedDescription(String title) { return format("direction.dreamCompleted", "title", title); }
    public String goalCreatedDescription(String title) { return format("direction.goalCreated", "title", title); }
    public String goalCompletedDescription(String title) { return format("direction.goalCompleted", "title", title); }
    public String dreamMilestoneCompletedDescription(String title) { return format("direction.dreamMilestoneCompleted", "title", title); }
    public String ritualCompletedDescription(String title) { return format("direction.ritualCompleted", "title", title); }
    public String nowSnapshotCreatedDescription() { return text("direction.nowSnapshotCreated"); }
    public String nowSnapshotDeletedDescription() { return text("direction.nowSnapshotDeleted"); }
    public String nowSongAddedDescription(int slot, String title) {
        return format("direction.nowSongAdded", "slot", slot).replace("{title}", title);
    }
    public String nowSongReplacedDescription(int slot, String title) {
        return format("direction.nowSongReplaced", "slot", slot).replace("{title}", title);
    }
    public String nowSongRemovedDescription(String title) { return format("direction.nowSongRemoved", "title", title); }
    public String nowBackgroundSetDescription() { return text("direction.nowBackgroundSet"); }
    public String nowBackgroundClearedDescription() { return text("direction.nowBackgroundCleared"); }
    public String nowStageItemAddedDescription(String verb, String title) {
        return format("direction.nowStageItemAdded", "verb", verb).replace("{title}", title);
    }
    public String nowStageItemRemovedDescription(String verb, String title) {
        return format("direction.nowStageItemRemoved", "verb", verb).replace("{title}", title);
    }
    public String nowImagesAddedDescription(int count) { return format("direction.nowImagesAdded", "count", count); }
    public String nowImagesRemovedDescription(int count) { return format("direction.nowImagesRemoved", "count", count); }
    public String nowDirectionPickedDescription(String title) { return format("direction.nowDirectionPicked", "title", title); }
    public String nowDirectionReleasedDescription(String title) { return format("direction.nowDirectionReleased", "title", title); }

    /** v0.6 life-module labels and fact summaries (content/life section). */
    public String lifeSleepTitle() { return text("life.sleepTitle"); }
    public String lifeSleepSummary(String duration, int quality) {
        return format("life.sleepSummary", "duration", duration).replace("{quality}", String.valueOf(quality));
    }
    public String lifeMealType(String type) { return text("life.mealType." + type); }
    public String lifeMealSummary(Integer satisfaction) {
        return satisfaction == null ? "" : format("life.mealSummary", "satisfaction", satisfaction);
    }
    public String lifeExerciseType(String type) { return text("life.exerciseType." + type); }
    public String lifeExerciseIntensity(String intensity) { return text("life.exerciseIntensity." + intensity); }
    public String lifeCheckInTitle() { return text("life.checkInTitle"); }
    public String lifeCheckInSummary(int energy, int mood, int focusDesire, int fatigue) {
        return format("life.checkInSummary", "energy", energy)
                .replace("{mood}", String.valueOf(mood))
                .replace("{focus}", String.valueOf(focusDesire))
                .replace("{fatigue}", String.valueOf(fatigue));
    }
    public String lifeRecordType(String type) { return text("life.lifeRecordType." + type); }
    public String lifeJournalTitle() { return text("life.journalTitle"); }

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
