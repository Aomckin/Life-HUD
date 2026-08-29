package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.LifeEvent;
import io.github.aomckin.lifehud.domain.LifeEventType;
import org.springframework.stereotype.Component;

/** The single, auditable entry point for v0.4 growth arithmetic. */
@Component
public final class GrowthRules {
    public Change evaluate(LifeEvent event) {
        if (Boolean.TRUE.equals(event.metadata().get("test"))) return new Change(0, 0, "测试记录不计入成长");
        if (event.type() == LifeEventType.FOCUS_FINISHED) {
            long seconds = number(event, "effectiveSeconds");
            long minutes = Math.max(0, seconds / 60);
            int exp = (int) Math.min(240, minutes / 5);
            int energy = (int) Math.min(12, minutes / 30);
            return new Change(exp, energy, "有效 Focus " + minutes + " 分钟");
        }
        if (event.type() == LifeEventType.TASK_COMPLETED) {
            int exp = clamp((int) number(event, "baseExp"), 0, 50);
            int energy = clamp((int) number(event, "baseEnergy"), -20, 20);
            return new Change(exp, energy, "完成任务");
        }
        return new Change(0, 0, "已记录生活事件");
    }
    private long number(LifeEvent event, String key) {
        Object value = event.metadata().get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public record Change(int exp, int energy, String reason) { }
}
