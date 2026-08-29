package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NowServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    @BeforeEach void setup() throws Exception { data=new TestDataSupport(temp);data.baseline(100,0,0);w=V05TestWiring.create(data); }

    private NowState state(String stage,String dreamId){
        return new NowState(stage,"热烈、开发、独居",
                w.now.current().favoriteSongs(),
                List.of(new NowItem("舞萌","","")),List.of(new NowItem("幼女战记","","")),
                List.of(new NowItem("没有黄笑话的小说","","")),
                dreamId==null?List.of():List.of(dreamId),List.of(),
                "走自己的路。",List.of(),"自由文字内容",null);
    }
    private MockMultipartFile song(String name){
        return new MockMultipartFile("file",name,"audio/mpeg",new byte[] {1,2,3,4});
    }

    @Test void currentStateStartsEmptyAndIsEditable(){
        assertThat(w.now.current().stageTitle()).isEmpty();
        NowState saved=w.now.update(state("2026 盛夏",null));
        assertThat(saved.stageTitle()).isEqualTo("2026 盛夏");
        assertThat(w.now.current().currentGames()).hasSize(1);
    }

    @Test void snapshotFreezesTitlesAndNeverChangesAfterwards(){
        Dream dream=w.dreams.create(new DreamRequest("做出真正属于自己的生活 Agent","", "让生活记录不再依赖手动维护。",null,null,"",""));
        w.now.update(state("2026 盛夏",dream.id()));
        NowSnapshot snapshot=w.now.createSnapshot();
        assertThat(snapshot.currentDreams()).hasSize(1);
        assertThat(snapshot.currentDreams().getFirst().snapshotTitle()).isEqualTo("做出真正属于自己的生活 Agent");
        assertThat(w.lifeEvents.all()).filteredOn(v->v.type()==LifeEventType.NOW_SNAPSHOT_CREATED).hasSize(1);
        // Edit the current Now and rename the dream: the snapshot must stay exactly as it was.
        w.now.update(state("2026 秋招",dream.id()));
        w.dreams.update(dream.id(),new DreamRequest("改名的梦想","",null,null,null,null,null));
        NowSnapshot reloaded=w.now.snapshot(snapshot.id());
        assertThat(reloaded.stageTitle()).isEqualTo("2026 盛夏");
        assertThat(reloaded.currentDreams().getFirst().snapshotTitle()).isEqualTo("做出真正属于自己的生活 Agent");
        assertThat(w.now.current().stageTitle()).isEqualTo("2026 秋招");
    }

    @Test void historyListsNewestFirstAndDeleteRemovesOnlyTarget(){
        w.now.update(state("第一期",null));
        NowSnapshot first=w.now.createSnapshot();
        w.now.update(state("第二期",null));
        NowSnapshot second=w.now.createSnapshot();
        assertThat(w.now.snapshots()).extracting(NowSnapshot::stageTitle).containsExactly("第二期","第一期");
        w.now.deleteSnapshot(first.id());
        assertThat(w.now.snapshots()).hasSize(1);assertThat(w.now.snapshots().getFirst().id()).isEqualTo(second.id());
    }

    @Test void songUploadOccupiesSlotAndCanBeRemoved(){
        NowState state=w.now.uploadSong(2,song("晴天.mp3"));
        assertThat(state.favoriteSongs()).hasSize(1);
        assertThat(state.favoriteSongs().getFirst().slot()).isEqualTo(2);
        assertThat(state.favoriteSongs().getFirst().title()).isEqualTo("晴天");
        assertThat(state.favoriteSongs().getFirst().artist()).isEqualTo("未知艺术家");
        // Replacing the same slot keeps exactly one song there.
        NowState replaced=w.now.uploadSong(2,song("another.mp3"));
        assertThat(replaced.favoriteSongs()).hasSize(1);
        assertThat(replaced.favoriteSongs().getFirst().title()).isEqualTo("another");
        NowState removed=w.now.removeSong(2);
        assertThat(removed.favoriteSongs()).isEmpty();
        assertThatThrownBy(()->w.now.uploadSong(11,song("x.mp3"))).hasMessageContaining("1 ~ 10");
        assertThatThrownBy(()->w.now.update(stateWithDuplicateSlots())).hasMessageContaining("重复");
    }

    /** Snapshots freeze the whole song records; later slot changes never rewrite them. */
    @Test void snapshotFreezesSongsBeforePlaylistChanges(){
        w.now.uploadSong(1,song("旧歌.mp3"));
        w.now.update(state("盛夏歌单",null));
        NowSnapshot snapshot=w.now.createSnapshot();
        assertThat(snapshot.favoriteSongs()).extracting(NowSong::title).containsExactly("旧歌");
        w.now.removeSong(1);
        w.now.uploadSong(1,song("新歌.mp3"));
        w.now.update(state("秋招歌单",null));
        NowSnapshot reloaded=w.now.snapshot(snapshot.id());
        assertThat(reloaded.stageTitle()).isEqualTo("盛夏歌单");
        assertThat(reloaded.favoriteSongs()).extracting(NowSong::title).containsExactly("旧歌");
        assertThat(w.now.current().favoriteSongs()).extracting(NowSong::title).containsExactly("新歌");
    }

    private NowState stateWithDuplicateSlots(){
        NowSong a=new NowSong(1,"/uploads/a.mp3","a.mp3","A","", "",0,"","MP3",null);
        NowSong b=new NowSong(1,"/uploads/b.mp3","b.mp3","B","", "",0,"","MP3",null);
        return new NowState("","",List.of(a,b),List.of(),List.of(),List.of(),List.of(),List.of(),"",List.of(),"",null);
    }
}
