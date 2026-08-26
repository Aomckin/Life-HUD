package io.github.aomckin.lifehud.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record GameCommand(String type, JsonNode payload) {}
