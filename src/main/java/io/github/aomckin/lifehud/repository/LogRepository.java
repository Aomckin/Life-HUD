package io.github.aomckin.lifehud.repository;

import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Persistence boundary for the append-only game log. */
@Repository
public final class LogRepository {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JsonFileStore files;

    public LogRepository(JsonFileStore files) { this.files = files; }
    public void action(String action, String change, int currentEnergy) {
        append(now(), action, "能量变化：" + change, "当前能量：" + currentEnergy);
    }
    public void timedAction(String name, int minutes, int energy, int exp, int current) {
        append(now(), "完成" + name + minutes + "分钟", "能量 " + String.format("%+d", energy),
                "经验 +" + exp, "当前能量：" + current);
    }
    public void abandon(String name, int minutes) { append(now(), "放弃" + name, "已进行" + minutes + "分钟"); }
    public void system(String content) { append(now(), "系统", content); }
    public List<String> recent() {
        List<String> lines = files.readLines("log.txt");
        return lines.subList(Math.max(0, lines.size() - 10), lines.size());
    }
    private void append(String... parts) { files.appendLine("log.txt", String.join("|", parts)); }
    private String now() { return LocalDateTime.now().format(FORMAT); }
}
