package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.repository.DreamRepository;
import io.github.aomckin.lifehud.repository.GoalRepository;
import io.github.aomckin.lifehud.repository.NowRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 「现在。」: one continuously editable current state plus immutable stage snapshots.
 * A snapshot deep-copies everything at save time — songs, playlist background, wall
 * layout included — so editing the current state afterwards never rewrites history.
 */
@Service
public final class NowService {
    /** The 歌单 is exactly ten slots by product definition, not by technical limit. */
    public static final int SONG_SLOTS = 10;
    private final NowRepository repository;
    private final DreamRepository dreams;
    private final GoalRepository goals;
    private final LifeEventService events;
    private final GrowthCopy copy;
    private final AudioStorageService audio;
    private final ImageStorageService images;

    public NowService(NowRepository repository, DreamRepository dreams, GoalRepository goals,
                      LifeEventService events, GrowthCopy copy, AudioStorageService audio, ImageStorageService images) {
        this.repository = repository; this.dreams = dreams; this.goals = goals;
        this.events = events; this.copy = copy; this.audio = audio; this.images = images;
    }

    public NowState current() { return repository.state().orElseGet(NowService::emptyState); }

    private static NowState emptyState() {
        return new NowState("", "", "", "", "", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", List.of(), "", Instant.now());
    }

    public synchronized NowState update(NowState request) {
        NowState previous = current();
        NowState value = new NowState(clean(request.stageTitle()), clean(request.theme()),
                clean(request.playlistBackgroundImage()), clean(request.playlistTitle()), clean(request.playlistSubtitle()),
                validSongs(request.favoriteSongs()), request.currentGames(), request.currentAnime(),
                request.currentBooks(), request.currentDreamIds(), request.currentGoalIds(),
                clean(request.favoriteQuote()), request.images(), request.content(), Instant.now());
        repository.saveState(value);
        recordStateChanges(previous, value);
        return value;
    }

    /**
     * Facts only reach the timeline: stage items, gallery images and direction picks
     * entering or leaving the current life. Wording edits (stage title, theme, quote,
     * content, playlist captions) stay private, as do pure layout re-flows.
     */
    private void recordStateChanges(NowState previous, NowState value) {
        recordStageList("玩", previous.currentGames(), value.currentGames());
        recordStageList("看", previous.currentAnime(), value.currentAnime());
        recordStageList("读", previous.currentBooks(), value.currentBooks());
        for (String path : listDifference(value.images(), previous.images()))
            events.record(LifeEventType.NOW_IMAGE_ADDED, "now", "照片",
                copy.nowImagesAddedDescription(1), List.of("now"), imageMetadata("ADDED", path));
        for (String path : listDifference(previous.images(), value.images()))
            events.record(LifeEventType.NOW_IMAGE_REMOVED, "now", "照片",
                copy.nowImagesRemovedDescription(1), List.of("now"), imageMetadata("REMOVED", path));
        for (String id : added(value.currentDreamIds(), previous.currentDreamIds()))
            dreams.find(id).ifPresent(dream -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", dream.title(),
                copy.nowDirectionPickedDescription(dream.title()), List.of("now"), directionMetadata("ADDED", "DREAM", id)));
        for (String id : added(previous.currentDreamIds(), value.currentDreamIds()))
            dreams.find(id).ifPresent(dream -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", dream.title(),
                copy.nowDirectionReleasedDescription(dream.title()), List.of("now"), directionMetadata("REMOVED", "DREAM", id)));
        for (String id : added(value.currentGoalIds(), previous.currentGoalIds()))
            goals.find(id).ifPresent(goal -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", goal.title(),
                copy.nowDirectionPickedDescription(goal.title()), List.of("now"), directionMetadata("ADDED", "GOAL", id)));
        for (String id : added(previous.currentGoalIds(), value.currentGoalIds()))
            goals.find(id).ifPresent(goal -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", goal.title(),
                copy.nowDirectionReleasedDescription(goal.title()), List.of("now"), directionMetadata("REMOVED", "GOAL", id)));
    }

    /** One fact per entering/leaving title; same-title edits (subtitle/note) stay private. */
    private void recordStageList(String verb, List<NowItem> previous, List<NowItem> value) {
        Set<String> before = titlesOf(previous);
        Set<String> after = titlesOf(value);
        for (NowItem item : value)
            if (item != null && !item.title().isBlank() && !before.contains(item.title()))
                events.record(LifeEventType.NOW_STAGE_ITEM_ADDED, "now", item.title(),
                    copy.nowStageItemAddedDescription(verb, item.title()), List.of("now"), itemMetadata("ADDED", verb, item));
        for (NowItem item : previous)
            if (item != null && !item.title().isBlank() && !after.contains(item.title()))
                events.record(LifeEventType.NOW_STAGE_ITEM_REMOVED, "now", item.title(),
                    copy.nowStageItemRemovedDescription(verb, item.title()), List.of("now"), itemMetadata("REMOVED", verb, item));
    }

    /** Multiset difference preserves order and handles a path repeated more than once. */
    private List<String> listDifference(List<String> values, List<String> baseline) {
        List<String> remaining = new ArrayList<>(baseline == null ? List.of() : baseline);
        List<String> difference = new ArrayList<>();
        for (String value : values == null ? List.<String>of() : values) {
            if (!remaining.remove(value)) difference.add(value);
        }
        return difference;
    }

    private Set<String> titlesOf(List<NowItem> items) {
        return items.stream().filter(Objects::nonNull).map(NowItem::title)
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<String> added(List<String> next, List<String> previous) {
        Set<String> before = new HashSet<>(previous);
        return next.stream().filter(id -> !before.contains(id)).toList();
    }

    /** Stores a real audio upload into one of the ten slots, replacing whatever occupied it. */
    public synchronized NowState uploadSong(int slot, MultipartFile file) {
        validateSlot(slot);
        NowState state = current();
        NowSong oldSong = state.favoriteSongs().stream().filter(song -> song.slot() == slot).findFirst().orElse(null);
        boolean replaced = oldSong != null;
        if (replaced) state = withoutSong(state, slot);
        NowSong song = audio.store(file, slot);
        NowState value = withSong(state, song);
        repository.saveState(value);
        events.record(replaced ? LifeEventType.NOW_SONG_REPLACED : LifeEventType.NOW_SONG_ADDED,
                "now", song.title(),
                replaced ? copy.nowSongReplacedDescription(slot, oldSong.title(), song.title())
                         : copy.nowSongAddedDescription(slot, song.title()),
                List.of("now", "playlist"), songChangeMetadata(replaced ? "REPLACED" : "ADDED", oldSong, song));
        return value;
    }

    /** Edits an existing placed song; null fields keep their current value. */
    public synchronized NowState updateSong(int slot, NowSongUpdate request) {
        validateSlot(slot);
        NowState state = current();
        NowSong old = state.favoriteSongs().stream().filter(s -> s.slot() == slot).findFirst()
                .orElseThrow(() -> bad("该位置还没有歌曲"));
        NowSong song = new NowSong(slot, old.filePath(), old.originalFilename(),
                request.title() == null ? old.title() : clean(request.title()),
                request.artist() == null ? old.artist() : clean(request.artist()),
                request.album() == null ? old.album() : clean(request.album()),
                old.durationSeconds(), old.coverPath(), old.format(),
                request.playCount() == null ? old.playCount() : Math.max(0, request.playCount()),
                request.note() == null ? old.note() : clean(request.note()),
                request.posX() == null ? old.posX() : clamp(request.posX(), 0, 1),
                request.posY() == null ? old.posY() : clamp(request.posY(), 0, 1),
                request.rotationDeg() == null ? old.rotationDeg() : clamp(request.rotationDeg(), -4, 4),
                request.zIndex() == null ? old.zIndex() : request.zIndex(),
                old.createdAt(), Instant.now());
        NowState value = withSong(state, song);
        repository.saveState(value);
        return value;
    }

    public synchronized NowState removeSong(int slot) {
        validateSlot(slot);
        NowState state = current();
        NowSong removed = state.favoriteSongs().stream().filter(s -> s.slot() == slot).findFirst().orElse(null);
        NowState value = withoutSong(state, slot);
        repository.saveState(value);
        if (removed != null) events.record(LifeEventType.NOW_SONG_REMOVED, "now", removed.title(),
                copy.nowSongRemovedDescription(removed.title()), List.of("now", "playlist"), songChangeMetadata("REMOVED", removed, null));
        return value;
    }

    /** Playlist background is a distinct field, stored through the shared image storage. */
    public synchronized NowState setBackground(MultipartFile file) {
        String path = images.store(file);
        NowState state = current();
        String oldPath = state.playlistBackgroundImage();
        NowState value = new NowState(state.stageTitle(), state.theme(), path, state.playlistTitle(),
                state.playlistSubtitle(), state.favoriteSongs(), state.currentGames(), state.currentAnime(),
                state.currentBooks(), state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(),
                state.images(), state.content(), Instant.now());
        repository.saveState(value);
        events.record(LifeEventType.NOW_BACKGROUND_CHANGED, "now", "舞台背景",
                copy.nowBackgroundSetDescription(), List.of("now"), backgroundMetadata(oldPath, path));
        return value;
    }

    public synchronized NowState clearBackground() {
        NowState state = current();
        String oldPath = state.playlistBackgroundImage();
        NowState value = new NowState(state.stageTitle(), state.theme(), "", state.playlistTitle(),
                state.playlistSubtitle(), state.favoriteSongs(), state.currentGames(), state.currentAnime(),
                state.currentBooks(), state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(),
                state.images(), state.content(), Instant.now());
        repository.saveState(value);
        if (!oldPath.isBlank()) events.record(LifeEventType.NOW_BACKGROUND_CHANGED, "now", "舞台背景",
                copy.nowBackgroundClearedDescription(), List.of("now"), backgroundMetadata(oldPath, ""));
        return value;
    }

    private NowState withSong(NowState state, NowSong song) {
        List<NowSong> songs = state.favoriteSongs().stream().filter(s -> s.slot() != song.slot())
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        songs.add(song);
        songs.sort(Comparator.comparingInt(NowSong::slot));
        return new NowState(state.stageTitle(), state.theme(), state.playlistBackgroundImage(), state.playlistTitle(),
                state.playlistSubtitle(), songs, state.currentGames(), state.currentAnime(), state.currentBooks(),
                state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(), state.images(),
                state.content(), Instant.now());
    }

    private NowState withoutSong(NowState state, int slot) {
        List<NowSong> songs = state.favoriteSongs().stream().filter(s -> s.slot() != slot).toList();
        return new NowState(state.stageTitle(), state.theme(), state.playlistBackgroundImage(), state.playlistTitle(),
                state.playlistSubtitle(), songs, state.currentGames(), state.currentAnime(), state.currentBooks(),
                state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(), state.images(),
                state.content(), Instant.now());
    }

    private void validateSlot(int slot) {
        if (slot < 1 || slot > SONG_SLOTS) throw bad("歌单位置必须在 1 ~ " + SONG_SLOTS + " 之间");
    }

    private List<NowSong> validSongs(List<NowSong> songs) {
        if (songs == null) return List.of();
        Set<Integer> seen = new HashSet<>();
        for (NowSong song : songs) {
            if (song.slot() < 1 || song.slot() > SONG_SLOTS) throw bad("歌单位置必须在 1 ~ " + SONG_SLOTS + " 之间");
            if (!seen.add(song.slot())) throw bad("歌单位置 " + song.slot() + " 重复");
        }
        return songs;
    }

    private Double clamp(Double value, double min, double max) {
        return value == null ? null : Math.max(min, Math.min(max, value));
    }

    /** Deep-copies the current state; dream/goal references freeze id + title at this moment. */
    public synchronized NowSnapshot createSnapshot() {
        NowState state = current();
        Instant now = Instant.now();
        NowSnapshot value = new NowSnapshot(UUID.randomUUID().toString(), state.stageTitle(), state.theme(),
                state.playlistBackgroundImage(), state.playlistTitle(), state.playlistSubtitle(),
                state.favoriteSongs(), state.currentGames(), state.currentAnime(), state.currentBooks(),
                state.currentDreamIds().stream().map(this::dreamRef).flatMap(Optional::stream).toList(),
                state.currentGoalIds().stream().map(this::goalRef).flatMap(Optional::stream).toList(),
                state.favoriteQuote(), state.images(), state.content(), now);
        repository.saveSnapshot(value);
        events.record(LifeEventType.NOW_SNAPSHOT_CREATED, "now", value.stageTitle().isBlank() ? "现在。" : value.stageTitle(),
                copy.nowSnapshotCreatedDescription(), List.of("now"), snapshotMetadata(value));
        return value;
    }

    public List<NowSnapshot> snapshots() { return repository.snapshots(); }
    public NowSnapshot snapshot(String id) { return repository.findSnapshot(id).orElseThrow(this::missingSnapshot); }

    public synchronized void deleteSnapshot(String id) {
        NowSnapshot snapshot = repository.findSnapshot(id).orElseThrow(this::missingSnapshot);
        if (!repository.deleteSnapshot(id)) throw missingSnapshot();
        events.record(LifeEventType.NOW_SNAPSHOT_DELETED, "now", snapshot.stageTitle().isBlank() ? "现在。" : snapshot.stageTitle(),
                copy.nowSnapshotDeletedDescription(), List.of("now"), snapshotMetadata(snapshot));
    }

    private Map<String, Object> imageMetadata(String action, String path) {
        return Map.of("action", action, "imagePath", path, "images", List.of(path));
    }

    private Map<String, Object> directionMetadata(String action, String type, String id) {
        return Map.of("action", action, "directionType", type, "directionId", id);
    }

    private Map<String, Object> itemMetadata(String action, String verb, NowItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", action); metadata.put("verb", verb);
        metadata.put("category", switch (verb) { case "玩" -> "GAME"; case "看" -> "ANIME"; default -> "BOOK"; });
        metadata.put("title", clean(item.title())); metadata.put("subtitle", clean(item.subtitle())); metadata.put("note", clean(item.note()));
        return metadata;
    }

    private Map<String, Object> songChangeMetadata(String action, NowSong before, NowSong after) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", action);
        NowSong relevant = after == null ? before : after;
        metadata.put("slot", relevant.slot());
        if (before != null) metadata.put("before", songMetadata(before));
        if (after != null) metadata.put("after", songMetadata(after));
        List<String> covers = new ArrayList<>();
        if (after != null && !clean(after.coverPath()).isBlank()) covers.add(after.coverPath());
        else if (before != null && !clean(before.coverPath()).isBlank()) covers.add(before.coverPath());
        if (!covers.isEmpty()) metadata.put("images", covers);
        return metadata;
    }

    private Map<String, Object> songMetadata(NowSong song) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("slot", song.slot()); metadata.put("title", clean(song.title()));
        metadata.put("artist", clean(song.artist())); metadata.put("album", clean(song.album()));
        metadata.put("filePath", clean(song.filePath())); metadata.put("coverPath", clean(song.coverPath()));
        return metadata;
    }

    private Map<String, Object> backgroundMetadata(String before, String after) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", clean(before).isBlank() ? "ADDED" : clean(after).isBlank() ? "REMOVED" : "REPLACED");
        metadata.put("beforePath", clean(before)); metadata.put("afterPath", clean(after));
        String relevant = clean(after).isBlank() ? clean(before) : clean(after);
        if (!relevant.isBlank()) metadata.put("images", List.of(relevant));
        return metadata;
    }

    private Map<String, Object> snapshotMetadata(NowSnapshot snapshot) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("snapshotId", snapshot.id()); metadata.put("sourceId", snapshot.id());
        metadata.put("stageTitle", clean(snapshot.stageTitle()));
        metadata.put("songCount", snapshot.favoriteSongs().size()); metadata.put("imageCount", snapshot.images().size());
        if (!snapshot.images().isEmpty()) metadata.put("images", snapshot.images());
        else if (!clean(snapshot.playlistBackgroundImage()).isBlank()) metadata.put("images", List.of(snapshot.playlistBackgroundImage()));
        return metadata;
    }

    private Optional<NowSnapshot.NowRef> dreamRef(String id) {
        return dreams.find(id).map(d -> new NowSnapshot.NowRef(d.id(), d.title()));
    }
    private Optional<NowSnapshot.NowRef> goalRef(String id) {
        return goals.find(id).map(g -> new NowSnapshot.NowRef(g.id(), g.title()));
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missingSnapshot() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "「现在。」快照不存在"); }
}
