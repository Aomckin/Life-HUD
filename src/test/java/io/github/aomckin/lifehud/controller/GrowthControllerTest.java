package io.github.aomckin.lifehud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.LifeHudApplication;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=LifeHudApplication.class) @AutoConfigureMockMvc
class GrowthControllerTest {
    private static final Path DATA;
    static { try { DATA=Files.createTempDirectory("lifehud-growth-api-"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("lifehud.data-dir",()->DATA.toString());}
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;

    @Test void growthMilestoneSnapshotAndCustomTitleApisWork() throws Exception {
        mvc.perform(get("/api/growth")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value("0.8.0"));
        String milestone=mvc.perform(post("/api/milestones").contentType("application/json").content("{\"title\":\"Life HUD v0.4\",\"occurredAt\":\"2026-08-29\",\"category\":\"项目\",\"pinned\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pinned").value(true)).andReturn().getResponse().getContentAsString();
        String milestoneId=mapper.readTree(milestone).path("id").asText();
        mvc.perform(patch("/api/milestones/{id}",milestoneId).contentType("application/json").content("{\"title\":\"Growth 完成\",\"occurredAt\":\"2026-08-29\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Growth 完成"));
        String title=mvc.perform(post("/api/titles").contentType("application/json").content("{\"name\":\"夏日施工队\",\"description\":\"在盛夏完成施工。\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String titleId=mapper.readTree(title).path("id").asText();
        mvc.perform(post("/api/titles/{id}/equip",titleId)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("夏日施工队"));
        mvc.perform(post("/api/titles/unequip")).andExpect(status().isOk()).andExpect(jsonPath("$.equipped").value(false));
        mvc.perform(get("/api/titles")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '"+titleId+"')].equipped").value(false));
        mvc.perform(get("/api/growth/snapshots")).andExpect(status().isOk()).andExpect(jsonPath("$[0].milestoneCount").value(1));
        mvc.perform(get("/api/growth/snapshots")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/milestones/{id}",milestoneId)).andExpect(status().isNoContent());
    }

    @Test void energySpendSettlesExpFromActualAmountAndRejectsInvalidInput() throws Exception {
        mvc.perform(post("/api/growth/energy/spend").contentType("application/json").content("{\"amount\":10,\"reason\":\"看完一部纪录片\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDelta").value(-10))
                .andExpect(jsonPath("$.actualDelta").value(-10))
                .andExpect(jsonPath("$.expGained").value(1));
        mvc.perform(get("/api/growth/energy-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'SPEND')].requestedDelta").value(-10));
        mvc.perform(post("/api/growth/energy/adjust").contentType("application/json").content("{\"delta\":5,\"reason\":\"修正\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expGained").value(0));
        mvc.perform(post("/api/growth/energy/spend").contentType("application/json").content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }
}
