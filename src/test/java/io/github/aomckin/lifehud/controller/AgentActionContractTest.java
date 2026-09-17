package io.github.aomckin.lifehud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.LifeHudApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LifeHudApplication.class)
@AutoConfigureMockMvc
class AgentActionContractTest {
    private static final Path DATA;
    static {
        try { DATA = Files.createTempDirectory("lifehud-agent-actions-"); }
        catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("lifehud.data-dir", DATA::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void sleepWriteAppearsInContextAndTimelineThenDeleteRemovesBoth() throws Exception {
        Instant wake = Instant.now().minusSeconds(60);
        String body = """
                {"sleepTime":"%s","wakeTime":"%s","quality":8,"type":"NIGHT","note":"contract"}
                """.formatted(wake.minusSeconds(8 * 3600), wake);
        String id = idOf(mvc.perform(post("/api/life/sleep").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(get("/api/agent/context/today")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep.id").value(id));
        mvc.perform(get("/api/timeline?type=SLEEP_RECORDED")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value(id));

        mvc.perform(delete("/api/life/sleep/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/agent/context/today")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sleep").doesNotExist());
        mvc.perform(get("/api/timeline?type=SLEEP_RECORDED")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void latestCheckInIsVisibleToStatusContext() throws Exception {
        String body = """
                {"energy":7,"mood":8,"focusDesire":6,"fatigue":3,"time":"%s","note":"ready"}
                """.formatted(Instant.now());
        String id = idOf(mvc.perform(post("/api/life/check-ins").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/agent/context/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.checkIn.id").value(id))
                .andExpect(jsonPath("$.status.checkIn.energy").value(7));
    }

    @Test
    void repeatingTaskCompletionDoesNotDuplicateFact() throws Exception {
        String id = idOf(mvc.perform(post("/api/task-pool/daily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Agent contract task\",\"note\":\"small next step\",\"energy\":3,\"exp\":0}"))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(get("/api/task-pool")).andExpect(status().isOk())
                .andExpect(jsonPath("$.daily[?(@.taskId == '%s')].note".formatted(id)).value("small next step"));

        mvc.perform(post("/api/task-directions/daily/{id}/complete", id)).andExpect(status().isOk());
        mvc.perform(post("/api/task-directions/daily/{id}/complete", id)).andExpect(status().isOk());
        MvcResult timeline = mvc.perform(get("/api/timeline?type=TASK_COMPLETED&size=100"))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = mapper.readTree(timeline.getResponse().getContentAsString());
        assertThat(items).filteredOn(item -> id.equals(item.path("metadata").path("taskId").asText())).hasSize(1);
    }

    @Test
    void animeSessionAppearsInMediaContextAndTimeline() throws Exception {
        String animeId = idOf(mvc.perform(post("/api/media/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Contract Anime\",\"totalEpisodes\":12,\"currentEpisode\":0,\"status\":\"WATCHING\"}"))
                .andExpect(status().isCreated()).andReturn());
        String sessionBody = """
                {"episodeStart":1,"episodeEnd":1,"watchedAt":"%s","durationMinutes":24,"note":"episode one"}
                """.formatted(Instant.now());
        String sessionId = idOf(mvc.perform(post("/api/media/anime/{id}/sessions", animeId)
                        .contentType(MediaType.APPLICATION_JSON).content(sessionBody))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(get("/api/agent/context/media")).andExpect(status().isOk())
                .andExpect(jsonPath("$.media.animeSessions[0].id").value(sessionId));
        mvc.perform(get("/api/timeline?type=ANIME_WATCHED")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value(sessionId));
    }

    @Test
    void malformedPayloadAndMissingTaskUseStableErrors() throws Exception {
        mvc.perform(post("/api/life/sleep").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("请求格式或字段值无效"));
        mvc.perform(delete("/api/task-pool/daily/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("每日任务不存在"));
    }

    private String idOf(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }
}
