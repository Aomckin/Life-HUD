package io.github.aomckin.lifehud.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;

public class Player {
    public int energy;
    public int exp;
    public List<String> unlocked_achievements = new ArrayList<>();
    public Map<String,Integer> action_counts = new LinkedHashMap<>();
    public int done_task_count;
    public int done_special_task_count;
    public int coin;
    public Map<String,Map<String,Integer>> shop_daily_purchases = new LinkedHashMap<>();
    public Map<String,Integer> shop_total_purchases = new LinkedHashMap<>();
    public String daily_double_exp_date = "";
    public int special_task_slots = 1;
    public List<String> unlocked_titles = new ArrayList<>();
    public String equipped_title = "";
    public int completed_timed_actions;
    /** Unspent tail of the Energy→EXP conversion pool; only SPEND adds to it, 0 <= value < energy_per_exp. */
    public int exp_conversion_remainder;
    /** Last date (ISO) the day-boundary Energy drift toward the midpoint was applied. */
    public String energy_drift_date = "";
    @JsonIgnore public int maxEnergy;
}
