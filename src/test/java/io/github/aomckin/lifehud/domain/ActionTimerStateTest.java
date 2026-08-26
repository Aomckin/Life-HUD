package io.github.aomckin.lifehud.domain;

import io.github.aomckin.lifehud.core.ActionCatalog;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.assertThat;
class ActionTimerStateTest {
 @Test void pauseResumeCancelAndCompletionMatchPython(){var t=new ActionTimerState("敲代码",1,2);assertThat(t.formatRemaining()).isEqualTo("00:02");assertThat(t.tick()).isTrue();t.pause();assertThat(t.tick()).isFalse();assertThat(t.remainingSeconds()).isEqualTo(1);t.resume();assertThat(t.tick()).isTrue();assertThat(t.isFinished()).isTrue();assertThat(t.formatRemaining()).isEqualTo("00:00");}
 @Test void cancelPreventsFurtherTicks(){var t=new ActionTimerState("学习",25);t.tick();t.cancel();assertThat(t.cancelled()).isTrue();assertThat(t.tick()).isFalse();assertThat(t.elapsedMinutes()).isZero();}
 @Test void durationOptionsHavePythonValues(){assertThat(ActionCatalog.POSITIVE_OPTIONS).containsExactly(new ActionDurationOption(25,1,0),new ActionDurationOption(45,1.5,0),new ActionDurationOption(60,2,0));assertThat(ActionCatalog.NEGATIVE_OPTIONS).containsExactly(new ActionDurationOption(30,1,1),new ActionDurationOption(60,1.5,2),new ActionDurationOption(90,2,3));}
}
