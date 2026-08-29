package io.github.aomckin.lifehud.domain;

/**
 * Editable fields of a placed song (PUT /api/now/songs/{slot}). null keeps the
 * existing value; playCount drives the visual size level, posX/posY the wall position.
 */
public record NowSongUpdate(String title, String artist, String album, Integer playCount, String note,
                            Double posX, Double posY, Double rotationDeg, Integer zIndex) { }
