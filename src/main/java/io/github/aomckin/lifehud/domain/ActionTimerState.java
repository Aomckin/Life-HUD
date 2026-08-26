package io.github.aomckin.lifehud.domain;

public final class ActionTimerState {
    private final String actionName;
    private final int durationMinutes;
    private final int totalSeconds;
    private int remainingSeconds;
    private boolean paused;
    private boolean cancelled;

    public ActionTimerState(String actionName, int durationMinutes) { this(actionName, durationMinutes, durationMinutes * 60); }
    public ActionTimerState(String actionName, int durationMinutes, int totalSeconds) {
        this.actionName=actionName; this.durationMinutes=durationMinutes; this.totalSeconds=totalSeconds; this.remainingSeconds=totalSeconds;
    }
    public boolean tick(){if(paused||cancelled||isFinished())return false;remainingSeconds--;return true;}
    public void pause(){paused=true;}
    public void resume(){if(!cancelled)paused=false;}
    public void cancel(){cancelled=true;paused=false;}
    public boolean isFinished(){return remainingSeconds<=0;}
    public int elapsedMinutes(){return (totalSeconds-remainingSeconds)/60;}
    public String formatRemaining(){return "%02d:%02d".formatted(remainingSeconds/60,remainingSeconds%60);}
    public String actionName(){return actionName;} public int durationMinutes(){return durationMinutes;} public int totalSeconds(){return totalSeconds;}
    public int remainingSeconds(){return remainingSeconds;} public boolean paused(){return paused;} public boolean cancelled(){return cancelled;}
}
