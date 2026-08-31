package io.github.aomckin.lifehud.domain;

import java.time.Instant;
import java.util.List;

/**
 * An immutable copy of the Now state at a moment worth keeping. Dream/Goal references store
 * id + snapshot title and songs are frozen whole (playCount, note, wall layout included),
 * so later edits never rewrite history. Audio/cover/background files are shared by path
 * and intentionally never overwritten in place.
 */
public record NowSnapshot(String id, String stageTitle, String theme, String playlistBackgroundImage,
                          String playlistTitle, String playlistSubtitle, List<NowSong> favoriteSongs,
                          List<NowItem> currentGames, List<NowItem> currentAnime, List<NowItem> currentBooks,
                          List<NowRef> currentDreams, List<NowRef> currentGoals, String favoriteQuote,
                          List<String> images, String content, List<String> favoriteQuotes,
                          List<String> thoughts, Instant createdAt) {
    public NowSnapshot {
        playlistBackgroundImage = playlistBackgroundImage == null ? "" : playlistBackgroundImage;
        playlistTitle = playlistTitle == null ? "" : playlistTitle;
        playlistSubtitle = playlistSubtitle == null ? "" : playlistSubtitle;
        favoriteSongs = favoriteSongs == null ? List.of() : List.copyOf(favoriteSongs);
        currentGames = currentGames == null ? List.of() : List.copyOf(currentGames);
        currentAnime = currentAnime == null ? List.of() : List.copyOf(currentAnime);
        currentBooks = currentBooks == null ? List.of() : List.copyOf(currentBooks);
        currentDreams = currentDreams == null ? List.of() : List.copyOf(currentDreams);
        currentGoals = currentGoals == null ? List.of() : List.copyOf(currentGoals);
        images = images == null ? List.of() : List.copyOf(images);
        favoriteQuotes = normalizeTexts(favoriteQuotes, favoriteQuote);
        thoughts = normalizeTexts(thoughts, content);
        favoriteQuote = favoriteQuotes.isEmpty() ? "" : favoriteQuotes.getFirst();
        content = thoughts.isEmpty() ? "" : thoughts.getFirst();
    }

    public NowSnapshot(String id,String stageTitle,String theme,String playlistBackgroundImage,String playlistTitle,
                       String playlistSubtitle,List<NowSong> favoriteSongs,List<NowItem> currentGames,
                       List<NowItem> currentAnime,List<NowItem> currentBooks,List<NowRef> currentDreams,
                       List<NowRef> currentGoals,String favoriteQuote,List<String> images,String content,Instant createdAt) {
        this(id,stageTitle,theme,playlistBackgroundImage,playlistTitle,playlistSubtitle,favoriteSongs,currentGames,
                currentAnime,currentBooks,currentDreams,currentGoals,favoriteQuote,images,content,
                favoriteQuote==null||favoriteQuote.isBlank()?List.of():List.of(favoriteQuote),
                content==null||content.isBlank()?List.of():List.of(content),createdAt);
    }

    private static List<String> normalizeTexts(List<String> values,String legacy) {
        List<String> clean=values==null?List.of():values.stream().filter(java.util.Objects::nonNull)
                .map(String::trim).filter(v->!v.isBlank()).toList();
        if(clean.isEmpty()&&legacy!=null&&!legacy.isBlank())return List.of(legacy.trim());
        return List.copyOf(clean);
    }

    /** Id plus the title frozen at snapshot time. */
    public record NowRef(String id, String snapshotTitle) { }
}
