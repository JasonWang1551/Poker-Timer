package com.firetv.model;

public class TournamentLevel {
    private String title;
    private int smallBlind;
    private int bigBlind;
    private int minutes;
    private final boolean breakLevel;
    private boolean bigBlindDoubleSmallBlind;
    private boolean indefiniteBreak;
    private boolean hasOwnTime;
    private boolean hasAnte;
    private boolean anteEqualsBigBlind;
    private int ante;

    public TournamentLevel(
            String title,
            int smallBlind,
            int bigBlind,
            int minutes,
            boolean breakLevel,
            boolean bigBlindDoubleSmallBlind,
            boolean indefiniteBreak,
            boolean hasOwnTime,
            boolean hasAnte,
            boolean anteEqualsBigBlind,
            int ante) {
        this.title = title;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.minutes = minutes;
        this.breakLevel = breakLevel;
        this.bigBlindDoubleSmallBlind = bigBlindDoubleSmallBlind;
        this.indefiniteBreak = indefiniteBreak;
        this.hasOwnTime = hasOwnTime;
        this.hasAnte = hasAnte;
        this.anteEqualsBigBlind = anteEqualsBigBlind;
        this.ante = ante;
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

    public boolean hasOwnTime() {
        return hasOwnTime;
    }

    public void setHasOwnTime(boolean enabled) {
        hasOwnTime = enabled;
    }

    public boolean hasAnte() {
        return !breakLevel && hasAnte;
    }

    public void setHasAnte(boolean enabled) {
        hasAnte = !breakLevel && enabled;
    }

    public boolean isAnteEqualToBigBlind() {
        return !breakLevel && anteEqualsBigBlind;
    }

    public void setAnteEqualsBigBlind(boolean enabled) {
        anteEqualsBigBlind = !breakLevel && enabled;
    }

    public int getAnte() {
        if (isAnteEqualToBigBlind()) {
            return bigBlind;
        }

        return ante;
    }

    public int getCustomAnte() {
        return ante;
    }

    public void setAnte(int ante) {
        this.ante = ante;
    }
}
