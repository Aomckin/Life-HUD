package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.domain.Dream;
import io.github.aomckin.lifehud.domain.Ritual;

public record DashboardCompanions(Dream dream, Ritual ritual, Media media) {
    public record Media(String id,String type,String title,String detail) { }
}
