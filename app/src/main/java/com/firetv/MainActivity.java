package com.firetv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerText();
            handler.postDelayed(this, 250);
        }
    };

    private TextView timerTextView;
    private TextView statusTextView;
    private long accumulatedElapsedMs = 0L;
    private long runStartedAtMs = 0L;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerTextView = findViewById(R.id.timer_text);
        statusTextView = findViewById(R.id.status_text);

        updateStatusText();
        updateTimerText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseTimer();
        handler.removeCallbacks(tickRunnable);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            pauseTimer();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            startTimer();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void startTimer() {
        if (isRunning) {
            return;
        }

        runStartedAtMs = SystemClock.elapsedRealtime();
        isRunning = true;
        updateStatusText();
    }

    private void pauseTimer() {
        if (!isRunning) {
            return;
        }

        accumulatedElapsedMs += SystemClock.elapsedRealtime() - runStartedAtMs;
        isRunning = false;
        updateStatusText();
        updateTimerText();
    }

    private void updateStatusText() {
        statusTextView.setText(isRunning ? R.string.timer_running : R.string.timer_paused);
    }

    private void updateTimerText() {
        long elapsedMs = accumulatedElapsedMs;
        if (isRunning) {
            elapsedMs += SystemClock.elapsedRealtime() - runStartedAtMs;
        }

        long totalSeconds = elapsedMs / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }
}
