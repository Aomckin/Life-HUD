package io.github.aomckin.lifehud.domain;

public record DashboardSelections(String dreamId, String ritualId, String mediaType, String mediaId) {
    public DashboardSelections { dreamId=clean(dreamId);ritualId=clean(ritualId);mediaType=clean(mediaType);mediaId=clean(mediaId); }
    public static DashboardSelections empty(){return new DashboardSelections("","","","");}
    private static String clean(String value){return value==null?"":value.trim();}
}
