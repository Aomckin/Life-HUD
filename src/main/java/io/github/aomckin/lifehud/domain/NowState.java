package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** The continuously editable "现在。" state for the current life stage. */
public record NowState(String stageTitle, String theme, String playlistBackgroundImage, String playlistTitle,
                       String playlistSubtitle, List<NowSong> favoriteSongs, List<NowItem> currentGames,
                       List<NowItem> currentAnime, List<NowItem> currentBooks, List<String> currentDreamIds,
                       List<String> currentGoalIds, String favoriteQuote, List<String> images, String content,
                       Instant updatedAt) {
    public NowState {
        playlistBackgroundImage = playlistBackgroundImage == null ? "" : playlistBackgroundImage;
        playlistTitle = playlistTitle == null ? "" : playlistTitle;
        playlistSubtitle = playlistSubtitle == null ? "" : playlistSubtitle;
        favoriteSongs = favoriteSongs == null ? List.of() : List.copyOf(favoriteSongs);
        currentGames = currentGames == null ? List.of() : List.copyOf(currentGames);
        currentAnime = currentAnime == null ? List.of() : List.copyOf(currentAnime);
        currentBooks = currentBooks == null ? List.of() : List.copyOf(currentBooks);
        currentDreamIds = currentDreamIds == null ? List.of() : List.copyOf(currentDreamIds);
        currentGoalIds = currentGoalIds == null ? List.of() : List.copyOf(currentGoalIds);
        images = images == null ? List.of() : List.copyOf(images);
    }
}
