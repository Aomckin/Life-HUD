package io.github.aomckin.lifehud.domain;

public record TaskRequirement(TaskSource source, String taskId, int count) {
    public TaskRequirement {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}