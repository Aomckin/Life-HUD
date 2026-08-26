package io.github.aomckin.lifehud.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aomckin.lifehud.domain.Player;
import org.springframework.stereotype.Repository;

/** Player persistence and legacy-save normalization. */
@Repository
public final class PlayerRepository {
    private final JsonFileStore files;
    private final ObjectMapper mapper;
    public PlayerRepository(JsonFileStore files, ObjectMapper mapper) { this.files = files; this.mapper = mapper; }

    public Player load(int defaultEnergy, int maxEnergy) {
        Player player;
        if (!files.exists("save.json")) { player = new Player(); player.energy = defaultEnergy; }
        else try { player = mapper.treeToValue(files.read("save.json"), Player.class); }
        catch (Exception e) { throw new IllegalStateException("存档格式无效", e); }
        player.maxEnergy = maxEnergy; normalize(player); return player;
    }
    public void save(Player player) { files.write("save.json", player); }

    private void normalize(Player p) {
        if (p.unlocked_achievements == null) p.unlocked_achievements = new java.util.ArrayList<>();
        if (p.action_counts == null) p.action_counts = new java.util.LinkedHashMap<>();
        if (p.shop_daily_purchases == null) p.shop_daily_purchases = new java.util.LinkedHashMap<>();
        if (p.shop_total_purchases == null) p.shop_total_purchases = new java.util.LinkedHashMap<>();
        if (p.unlocked_titles == null) p.unlocked_titles = new java.util.ArrayList<>();
        if (p.daily_double_exp_date == null) p.daily_double_exp_date = "";
        if (p.equipped_title == null) p.equipped_title = "";
        if (p.special_task_slots < 1) p.special_task_slots = 1;
    }
}
