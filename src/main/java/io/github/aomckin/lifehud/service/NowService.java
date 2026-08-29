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
        recordListDelta(previous.images(), value.images(),
            count -> events.record(LifeEventType.NOW_IMAGE_ADDED, "now", "照片",
                copy.nowImagesAddedDescription(count), List.of("now"), Map.of("count", count)),
            count -> events.record(LifeEventType.NOW_IMAGE_REMOVED, "now", "照片",
                copy.nowImagesRemovedDescription(count), List.of("now"), Map.of("count", count)));
        for (String id : added(value.currentDreamIds(), previous.currentDreamIds()))
            dreams.find(id).ifPresent(dream -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", dream.title(),
                copy.nowDirectionPickedDescription(dream.title()), List.of("now"), Map.of("dreamId", id)));
        for (String id : added(previous.currentDreamIds(), value.currentDreamIds()))
            dreams.find(id).ifPresent(dream -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", dream.title(),
                copy.nowDirectionReleasedDescription(dream.title()), List.of("now"), Map.of("dreamId", id)));
        for (String id : added(value.currentGoalIds(), previous.currentGoalIds()))
            goals.find(id).ifPresent(goal -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", goal.title(),
                copy.nowDirectionPickedDescription(goal.title()), List.of("now"), Map.of("goalId", id)));
        for (String id : added(previous.currentGoalIds(), value.currentGoalIds()))
            goals.find(id).ifPresent(goal -> events.record(LifeEventType.NOW_DIRECTION_CHANGED, "now", goal.title(),
                copy.nowDirectionReleasedDescription(goal.title()), List.of("now"), Map.of("goalId", id)));
    }

    /** One fact per entering/leaving title; same-title edits (subtitle/note) stay private. */
    private void recordStageList(String verb, List<NowItem> previous, List<NowItem> value) {
        Set<String> before = titlesOf(previous);
        Set<String> after = titlesOf(value);
        for (NowItem item : value)
            if (item != null && !item.title().isBlank() && !before.contains(item.title()))
                events.record(LifeEventType.NOW_STAGE_ITEM_ADDED, "now", item.title(),
                    copy.nowStageItemAddedDescription(verb, item.title()), List.of("now"), Map.of());
        for (NowItem item : previous)
            if (item != null && !item.title().isBlank() && !after.contains(item.title()))
                events.record(LifeEventType.NOW_STAGE_ITEM_REMOVED, "now", item.title(),
                    copy.nowStageItemRemovedDescription(verb, item.title()), List.of("now"), Map.of());
    }

    private void recordListDelta(List<String> previous, List<String> value,
                                 java.util.function.IntConsumer added, java.util.function.IntConsumer removed) {
        int grew = value.size() - previous.size();
        if (grew > 0) added.accept(grew);
        else if (grew < 0) removed.accept(-grew);
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
        boolean replaced = state.favoriteSongs().stream().anyMatch(song -> song.slot() == slot);
        if (replaced) state = withoutSong(state, slot);
        NowSong song = audio.store(file, slot);
        NowState value = withSong(state, song);
        repository.saveState(value);
        events.record(replaced ? LifeEventType.NOW_SONG_REPLACED : LifeEventType.NOW_SONG_ADDED,
                "now", song.title(),
                replaced ? copy.nowSongReplacedDescription(slot, song.title())
                         : copy.nowSongAddedDescription(slot, song.title()),
                List.of("now", "playlist"), Map.of("slot", slot));
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
        String title = state.favoriteSongs().stream().filter(s -> s.slot() == slot)
                .findFirst().map(NowSong::title).orElse(null);
        NowState value = withoutSong(state, slot);
        repository.saveState(value);
        if (title != null) events.record(LifeEventType.NOW_SONG_REMOVED, "now", title,
                copy.nowSongRemovedDescription(title), List.of("now", "playlist"), Map.of("slot", slot));
        return value;
    }

    /** Playlist background is a distinct field, stored through the shared image storage. */
    public synchronized NowState setBackground(MultipartFile file) {
        String path = images.store(file);
        NowState state = current();
        NowState value = new NowState(state.stageTitle(), state.theme(), path, state.playlistTitle(),
                state.playlistSubtitle(), state.favoriteSongs(), state.currentGames(), state.currentAnime(),
                state.currentBooks(), state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(),
                state.images(), state.content(), Instant.now());
        repository.saveState(value);
        events.record(LifeEventType.NOW_BACKGROUND_CHANGED, "now", "舞台背景",
                copy.nowBackgroundSetDescription(), List.of("now"), Map.of());
        return value;
    }

    public synchronized NowState clearBackground() {
        NowState state = current();
        NowState value = new NowState(state.stageTitle(), state.theme(), "", state.playlistTitle(),
                state.playlistSubtitle(), state.favoriteSongs(), state.currentGames(), state.currentAnime(),
                state.currentBooks(), state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(),
                state.images(), state.content(), Instant.now());
        repository.saveState(value);
        events.record(LifeEventType.NOW_BACKGROUND_CHANGED, "now", "舞台背景",
                copy.nowBackgroundClearedDescription(), List.of("now"), Map.of());
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
                copy.nowSnapshotCreatedDescription(), List.of("now"), Map.of("snapshotId", value.id(), "sourceId", value.id()));
        return value;
    }

    public List<NowSnapshot> snapshots() { return repository.snapshots(); }
    public NowSnapshot snapshot(String id) { return repository.findSnapshot(id).orElseThrow(this::missingSnapshot); }

    public synchronized void deleteSnapshot(String id) {
        if (!repository.deleteSnapshot(id)) throw missingSnapshot();
        events.record(LifeEventType.NOW_SNAPSHOT_DELETED, "now", "快照",
                copy.nowSnapshotDeletedDescription(), List.of("now"), Map.of("snapshotId", id, "sourceId", id));
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
