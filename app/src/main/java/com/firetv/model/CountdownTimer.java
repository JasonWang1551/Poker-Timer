package com.firetv.model;

import android.os.SystemClock;

public class CountdownTimer {
    private long accumulatedElapsedMs;
    private long runStartedAtMs;
    private boolean running;

    public void start() {
        if (running) {
            return;
        }

        runStartedAtMs = SystemClock.elapsedRealtime();
        running = true;
    }

    public void pause() {
        if (!running) {
            return;
        }

        accumulatedElapsedMs += SystemClock.elapsedRealtime() - runStartedAtMs;
        running = false;
    }

    public void reset(boolean resume) {
        accumulatedElapsedMs = 0L;
        runStartedAtMs = SystemClock.elapsedRealtime();
        running = resume;
    }

    public void finish(long durationMs) {
        accumulatedElapsedMs = durationMs;
        running = false;
    }

    public long getRemainingMs(long durationMs) {
        long elapsedMs = accumulatedElapsedMs;

        if (running) {
            elapsedMs += SystemClock.elapsedRealtime() - runStartedAtMs;
        }

        return Math.max(0L, durationMs - elapsedMs);
    }

    public boolean isExpired(long durationMs) {
        return getRemainingMs(durationMs) == 0L;
    }

    public boolean isRunning() {
        return running;
    }
}
