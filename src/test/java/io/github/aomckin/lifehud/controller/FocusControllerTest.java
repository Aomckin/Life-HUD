package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.LifeHudApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LifeHudApplication.class)
@AutoConfigureMockMvc
class FocusControllerTest {
    private static final Path DATA;
    static {
        try { DATA = Files.createTempDirectory("lifehud-focus-api-"); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("lifehud.data-dir", () -> DATA.toString());
    }
    @Autowired MockMvc mvc;

    @BeforeEach void resetFocusData() throws Exception {
        Files.deleteIfExists(DATA.resolve("focus-sessions.json"));
        Files.deleteIfExists(DATA.resolve("life-events.json"));
    }

    @Test void startPauseResumeCompleteAndReadViews() throws Exception {
        String response = mvc.perform(post("/api/focus/start").contentType("application/json")
                        .content("{\"mode\":\"POMODORO\",\"title\":\"开发 v0.3\",\"plannedMinutes\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.plannedMinutes").value(25))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).path("id").asText();

        mvc.perform(get("/api/focus/current")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/api/focus/{id}/pause", id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"));
        mvc.perform(post("/api/focus/{id}/resume", id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post("/api/focus/{id}/complete", id).contentType("application/json").content("{\"note\":\"完成\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/focus/today")).andExpect(status().isOk()).andExpect(jsonPath("$.sessionCount").value(1));
        mvc.perform(get("/api/focus/history")).andExpect(status().isOk()).andExpect(jsonPath("$[0].note").value("完成"));
    }

    @Test void rejectsSecondConcurrentSession() throws Exception {
        String body = "{\"mode\":\"FREE\",\"title\":\"阅读\"}";
        mvc.perform(post("/api/focus/start").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/api/focus/start").contentType("application/json").content(body)).andExpect(status().isConflict());
    }
}
