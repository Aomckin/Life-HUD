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
        NowState value = new NowState(clean(request.stageTitle()), clean(request.theme()),
                clean(request.playlistBackgroundImage()), clean(request.playlistTitle()), clean(request.playlistSubtitle()),
                validSongs(request.favoriteSongs()), request.currentGames(), request.currentAnime(),
                request.currentBooks(), request.currentDreamIds(), request.currentGoalIds(),
                clean(request.favoriteQuote()), request.images(), request.content(), Instant.now());
        repository.saveState(value);
        return value;
    }

    /** Stores a real audio upload into one of the ten slots, replacing whatever occupied it. */
    public synchronized NowState uploadSong(int slot, MultipartFile file) {
        validateSlot(slot);
        NowState state = current();
        if (state.favoriteSongs().stream().anyMatch(song -> song.slot() == slot))
            state = withoutSong(state, slot);
        NowSong song = audio.store(file, slot);
        NowState value = withSong(state, song);
        repository.saveState(value);
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
        NowState value = withoutSong(current(), slot);
        repository.saveState(value);
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
        return value;
    }

    public synchronized NowState clearBackground() {
        NowState state = current();
        NowState value = new NowState(state.stageTitle(), state.theme(), "", state.playlistTitle(),
                state.playlistSubtitle(), state.favoriteSongs(), state.currentGames(), state.currentAnime(),
                state.currentBooks(), state.currentDreamIds(), state.currentGoalIds(), state.favoriteQuote(),
                state.images(), state.content(), Instant.now());
        repository.saveState(value);
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
