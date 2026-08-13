package com.firetv;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Build;
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
    private boolean allowDpadMediaFallback;
    private boolean hasAlerted = false;

     private final long currentTimerAmount = 600_000L;
    //private final long currentTimerAmount = 5_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerTextView = findViewById(R.id.timer_text);
        statusTextView = findViewById(R.id.status_text);
        allowDpadMediaFallback = !isAmazonFireTv();

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

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || (allowDpadMediaFallback && keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
            if (isRunning) {
                pauseTimer();
            }
            else {
                startTimer();
            }
            return true;
        }

        if ((keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND)
                || (allowDpadMediaFallback
                    && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT))) {
            resetTimer();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean isAmazonFireTv() {
        return "Amazon".equalsIgnoreCase(Build.MANUFACTURER);
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

    private void resetTimer() {
        accumulatedElapsedMs = 0L;
        runStartedAtMs = SystemClock.elapsedRealtime();
        hasAlerted = false;
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

        if (elapsedMs >= currentTimerAmount) {
            timerTextView.setText("00:00");
            if (!hasAlerted) {
                hasAlerted = true;
                playTimerAlert();
                resetTimer();
            }
            return;
        }

        long totalSeconds = currentTimerAmount / 1000L - elapsedMs / 1000L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void playTimerAlert() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2_000);
        handler.postDelayed(toneGenerator::release, 2_100);
    }
}
