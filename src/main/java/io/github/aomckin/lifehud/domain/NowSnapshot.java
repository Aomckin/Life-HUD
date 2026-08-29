package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/**
 * An immutable copy of the Now state at a moment worth keeping. Dream/Goal references store
 * id + snapshot title and songs are frozen whole, so later edits never rewrite history.
 * Audio/cover files are shared by path and intentionally never overwritten in place.
 */
public record NowSnapshot(String id, String stageTitle, String theme, List<NowSong> favoriteSongs,
                          List<NowItem> currentGames, List<NowItem> currentAnime, List<NowItem> currentBooks,
                          List<NowRef> currentDreams, List<NowRef> currentGoals, String favoriteQuote,
                          List<String> images, String content, Instant createdAt) {
    public NowSnapshot {
        favoriteSongs = favoriteSongs == null ? List.of() : List.copyOf(favoriteSongs);
        currentGames = currentGames == null ? List.of() : List.copyOf(currentGames);
        currentAnime = currentAnime == null ? List.of() : List.copyOf(currentAnime);
        currentBooks = currentBooks == null ? List.of() : List.copyOf(currentBooks);
        currentDreams = currentDreams == null ? List.of() : List.copyOf(currentDreams);
        currentGoals = currentGoals == null ? List.of() : List.copyOf(currentGoals);
        images = images == null ? List.of() : List.copyOf(images);
    }

    /** Id plus the title frozen at snapshot time. */
    public record NowRef(String id, String snapshotTitle) { }
}
