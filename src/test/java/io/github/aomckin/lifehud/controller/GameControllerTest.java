package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.LifeHudApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LifeHudApplication.class)
@AutoConfigureMockMvc
class GameControllerTest {
    private static final Path DATA;

    static {
        try { DATA = Files.createTempDirectory("lifehud-api-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("lifehud.data-dir", () -> DATA.toString());
    }

    @Autowired MockMvc mvc;

    @Test void stateReturnsFullQueryViewModel() throws Exception {
        mvc.perform(get("/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energy_value").isNumber())
                .andExpect(jsonPath("$.action_views.length()").value(6))
                .andExpect(jsonPath("$.achievement_sections.length()").value(2));
    }

    @Test void homeAndStaticAssetsAreServed() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/static/v0.2.html"));
        mvc.perform(get("/static/v0.2.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Life HUD v0.4")));
        mvc.perform(get("/static/app-v02.js")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pages/growth.js")));
        mvc.perform(get("/static/styles/growth.css")).andExpect(status().isOk());
    }

    @Test void durationAndCommandUseOriginalRoutes() throws Exception {
        mvc.perform(get("/actions/{name}/duration-options", "看番"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].type").value("ACTION_DURATION_OPTIONS"));
        mvc.perform(post("/command").contentType("application/json")
                        .content("{\"type\":\"UNKNOWN\",\"payload\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.events[0].type").value("ERROR"));
    }

    @Test void removedBusinessRoutesStayAbsent() throws Exception {
        mvc.perform(post("/actions/学习")).andExpect(status().isNotFound());
        mvc.perform(post("/tasks/0/complete")).andExpect(status().isNotFound());
        mvc.perform(post("/shop/x/buy")).andExpect(status().isNotFound());
    }
}
