package io.github.aomckin.lifehud.controller;

import io.github.aomckin.lifehud.LifeHudApplication;
import io.github.aomckin.lifehud.domain.Player;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=LifeHudApplication.class)
@AutoConfigureMockMvc
class AgentContextControllerTest {
    private static final Path DATA;
    static { try { DATA=Files.createTempDirectory("lifehud-agent-context-"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("lifehud.data-dir",DATA::toString);}
    @Autowired MockMvc mvc;
    @Autowired Player player;

    @Test void todayAndDashboardAreSafeWithNoLifeRecords() throws Exception {
        mvc.perform(get("/api/agent/context/today")).andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.generatedAt").exists()).andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.timeline").isArray());
        mvc.perform(get("/api/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").exists()).andExpect(jsonPath("$.headline").isString())
                .andExpect(jsonPath("$.headlines").isArray()).andExpect(jsonPath("$.focus").exists());
    }

    @Test void exposesAllReadOnlyContextViews() throws Exception {
        for(String path:new String[]{"status","focus","tasks","dreams","life","journal","media","growth"})
            mvc.perform(get("/api/agent/context/"+path)).andExpect(status().isOk()).andExpect(jsonPath("$.schemaVersion").value("1"));
    }

    @Test void rejectsUnboundedRangeAndLimit() throws Exception {
        mvc.perform(get("/api/agent/context/recent?days=31")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("days 必须在 1 到 30 之间"))
                .andExpect(jsonPath("$.path").value("/api/agent/context/recent"));
        mvc.perform(get("/api/agent/context/journal?limit=101")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("limit 必须在 1 到 100 之间"));
        mvc.perform(get("/api/agent/context/recent?days=not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("请求格式或字段值无效"));
    }

    @Test void dashboardSelectionsStartEmptyAndRejectMissingTargets() throws Exception {
        mvc.perform(get("/api/dashboard/selections")).andExpect(status().isOk())
                .andExpect(jsonPath("$.dreamId").value(""))
                .andExpect(jsonPath("$.ritualId").value(""))
                .andExpect(jsonPath("$.mediaId").value(""));
        mvc.perform(put("/api/dashboard/selections/dream").contentType("application/json")
                        .content("{\"id\":\"missing\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("展示对象不存在"));
    }

    @Test void resolvesEquippedTitleIdToCatalogName() throws Exception {
        String previous=player.equipped_title;
        try {
            player.equipped_title="iron_walker";
            mvc.perform(get("/api/agent/context/today")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.title").value("铁幕行者"));
            mvc.perform(get("/api/agent/context/growth")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.growth.title").value("铁幕行者"));
        } finally { player.equipped_title=previous; }
    }
}
