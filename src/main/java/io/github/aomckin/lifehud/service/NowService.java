package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.*;
import io.github.aomckin.lifehud.domain.Dream;
import io.github.aomckin.lifehud.domain.Goal;
import io.github.aomckin.lifehud.domain.NowSnapshot;
import io.github.aomckin.lifehud.domain.NowState;
import io.github.aomckin.lifehud.repository.DreamRepository;
import io.github.aomckin.lifehud.repository.GoalRepository;
import io.github.aomckin.lifehud.repository.NowRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 「现在。」: one continuously editable current state plus immutable stage snapshots.
 * A snapshot deep-copies everything at save time — editing the current state afterwards
 * must never rewrite history.
 */
@Service
public final class NowService {
    private final NowRepository repository;
    private final DreamRepository dreams;
    private final GoalRepository goals;
    private final LifeEventService events;
    private final GrowthCopy copy;

    public NowService(NowRepository repository, DreamRepository dreams, GoalRepository goals,
                      LifeEventService events, GrowthCopy copy) {
        this.repository = repository; this.dreams = dreams; this.goals = goals;
        this.events = events; this.copy = copy;
    }

    public NowState current() { return repository.state().orElseGet(NowService::emptyState); }

    private static NowState emptyState() {
        return new NowState("", "", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "", List.of(), "", Instant.now());
    }

    public synchronized NowState update(NowState request) {
        NowState value = new NowState(clean(request.stageTitle()), clean(request.theme()),
                request.favoriteSongs(), request.currentGames(), request.currentAnime(), request.currentBooks(),
                request.currentDreamIds(), request.currentGoalIds(), clean(request.favoriteQuote()),
                request.images(), request.content(), Instant.now());
        repository.saveState(value);
        return value;
    }

    /** Deep-copies the current state; dream/goal references freeze id + title at this moment. */
    public synchronized NowSnapshot createSnapshot() {
        NowState state = current();
        Instant now = Instant.now();
        NowSnapshot value = new NowSnapshot(UUID.randomUUID().toString(), state.stageTitle(), state.theme(),
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
    private ResponseStatusException missingSnapshot() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "「现在。」快照不存在"); }
}
