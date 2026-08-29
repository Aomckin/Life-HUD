package io.github.aomckin.lifehud.domain;

/** First-version entertainment categories; the future Media module may take some of these over. */
public enum EntertainmentCategory {
    GAME, ANIME, MOVIE, VIDEO, SOCIAL, OUTING, OTHER;

    /** Lenient parsing for request payloads; unknown or missing values fall back to OTHER. */
    public static EntertainmentCategory from(Object value) {
        if (value instanceof EntertainmentCategory category) return category;
        if (value != null) {
            try { return EntertainmentCategory.valueOf(String.valueOf(value).trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        return OTHER;
    }
}
