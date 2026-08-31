package io.github.aomckin.lifehud.dto;

import io.github.aomckin.lifehud.dto.agent.AgentContext;
import java.time.Instant;
import java.time.LocalDate;

/** Human-facing projection of the same aggregation used by Agent Context. */
public record DashboardSummary(Instant generatedAt, LocalDate date, String headline,
                               java.util.List<io.github.aomckin.lifehud.domain.DashboardHeadline> headlines, AgentContext.Status status,
                               AgentContext.Focus focus, AgentContext.Tasks tasks,
                               AgentContext.Life life, AgentContext.Dreams dreams,
                               AgentContext.Rituals rituals, AgentContext.Media media,
                               io.github.aomckin.lifehud.domain.DashboardSelections selections,
                               DashboardCompanions companions,
                               java.util.List<io.github.aomckin.lifehud.domain.TimelineItem> timeline) { }
