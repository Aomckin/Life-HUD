package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/** The continuously editable "现在。" state for the current life stage. */
public record NowState(String stageTitle, String theme, String playlistBackgroundImage, String playlistTitle,
                       String playlistSubtitle, List<NowSong> favoriteSongs, List<NowItem> currentGames,
                       List<NowItem> currentAnime, List<NowItem> currentBooks, List<String> currentDreamIds,
                       List<String> currentGoalIds, String favoriteQuote, List<String> images, String content,
                       List<String> favoriteQuotes, List<String> thoughts, Instant updatedAt) {
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
        favoriteQuotes = normalizeTexts(favoriteQuotes, favoriteQuote);
        thoughts = normalizeTexts(thoughts, content);
        favoriteQuote = favoriteQuotes.isEmpty() ? "" : favoriteQuotes.getFirst();
        content = thoughts.isEmpty() ? "" : thoughts.getFirst();
    }

    /** Source and JSON compatibility for v0.5-v0.7 single-value fields. */
    public NowState(String stageTitle,String theme,String playlistBackgroundImage,String playlistTitle,
                    String playlistSubtitle,List<NowSong> favoriteSongs,List<NowItem> currentGames,
                    List<NowItem> currentAnime,List<NowItem> currentBooks,List<String> currentDreamIds,
                    List<String> currentGoalIds,String favoriteQuote,List<String> images,String content,Instant updatedAt) {
        this(stageTitle,theme,playlistBackgroundImage,playlistTitle,playlistSubtitle,favoriteSongs,currentGames,
                currentAnime,currentBooks,currentDreamIds,currentGoalIds,favoriteQuote,images,content,
                favoriteQuote==null||favoriteQuote.isBlank()?List.of():List.of(favoriteQuote),
                content==null||content.isBlank()?List.of():List.of(content),updatedAt);
    }

    private static List<String> normalizeTexts(List<String> values,String legacy) {
        List<String> clean = values == null ? new java.util.ArrayList<>() : values.stream()
                .filter(java.util.Objects::nonNull).map(String::trim).filter(v->!v.isBlank()).toList();
        if(clean.isEmpty()&&legacy!=null&&!legacy.isBlank())return List.of(legacy.trim());
        return List.copyOf(clean);
    }
}
