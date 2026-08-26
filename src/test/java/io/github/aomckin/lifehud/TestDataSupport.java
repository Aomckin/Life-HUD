package io.github.aomckin.lifehud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.aomckin.lifehud.repository.*;
import java.nio.file.Path;
import java.time.LocalDate;

public final class TestDataSupport {
    public final ObjectMapper mapper=new ObjectMapper();public final JsonRepository json;public final PlayerRepository players;public final LogRepository logs;
    public TestDataSupport(Path root) throws Exception {json=new JsonRepository(mapper,root.toString());json.initialize();players=new PlayerRepository(json,mapper);logs=new LogRepository(json);normalizeDates();}
    public void normalizeDates(){for(String file:new String[]{"tasks.json","special_tasks.json"}){ObjectNode data=(ObjectNode)json.read(file);data.put("last_update_date",LocalDate.now().toString());json.write(file,data);}}
    public ObjectNode save(){return(ObjectNode)json.read("save.json");}public void writeSave(ObjectNode save){json.write("save.json",save);}public TestGameContext core(){return TestGameContext.create(json,players,logs,mapper);}
    public void baseline(int energy,int exp,int coin){ObjectNode s=save();s.put("energy",energy).put("exp",exp).put("coin",coin).put("done_task_count",0).put("done_special_task_count",0).put("completed_timed_actions",0).put("special_task_slots",1).put("daily_double_exp_date","").put("equipped_title","");s.withArray("unlocked_achievements").removeAll();s.withArray("unlocked_titles").removeAll();s.withObject("action_counts").removeAll();s.withObject("shop_daily_purchases").removeAll();s.withObject("shop_total_purchases").removeAll();writeSave(s);}
    public void markAllUnlockablesUnlocked(){ObjectNode s=save();s.withArray("unlocked_achievements").removeAll();json.read("achievements.json").forEach(i->s.withArray("unlocked_achievements").add(i.path("id").asText()));s.withArray("unlocked_titles").removeAll();json.read("titles.json").forEach(i->s.withArray("unlocked_titles").add(i.path("id").asText()));writeSave(s);}
}
