package com.firetv.model;

public class TournamentLevel {
    private String title;
    private int smallBlind;
    private int bigBlind;
    private int minutes;
    private final boolean breakLevel;
    private boolean bigBlindDoubleSmallBlind;
    private boolean indefiniteBreak;

    public TournamentLevel(
            String title,
            int smallBlind,
            int bigBlind,
            int minutes,
            boolean breakLevel,
            boolean bigBlindDoubleSmallBlind,
            boolean indefiniteBreak) {
        this.title = title;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.minutes = minutes;
        this.breakLevel = breakLevel;
        this.bigBlindDoubleSmallBlind = bigBlindDoubleSmallBlind;
        this.indefiniteBreak = indefiniteBreak;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public void setSmallBlind(int smallBlind) {
        this.smallBlind = smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public void setBigBlind(int bigBlind) {
        this.bigBlind = bigBlind;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public boolean isBreak() {
        return breakLevel;
    }

    public boolean isBigBlindDoubleSmallBlind() {
        return bigBlindDoubleSmallBlind;
    }

    public void setBigBlindDoubleSmallBlind(boolean enabled) {
        bigBlindDoubleSmallBlind = enabled;
    }

    public boolean isIndefiniteBreak() {
        return breakLevel && indefiniteBreak;
    }

    public void setIndefiniteBreak(boolean indefinite) {
        indefiniteBreak = breakLevel && indefinite;
    }
}
