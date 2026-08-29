package io.github.aomckin.lifehud.domain;

/** Resolved display titles for a Task's optional Direction links; dangling ids resolve to empty strings. */
public record DirectionInfo(String dreamId, String dreamTitle, String goalId, String goalTitle,
                            String dreamMilestoneId, String dreamMilestoneTitle) {
    public boolean empty() { return (dreamTitle == null || dreamTitle.isBlank())
            && (goalTitle == null || goalTitle.isBlank())
            && (dreamMilestoneTitle == null || dreamMilestoneTitle.isBlank()); }
}
