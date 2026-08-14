package com.firetv.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.firetv.R;
import com.firetv.controller.RemoteKeys;
import com.firetv.controller.TournamentEditorController;
import com.firetv.model.CountdownTimer;
import com.firetv.model.Tournament;
import com.firetv.model.TournamentLevel;
import com.firetv.model.TournamentStore;

import java.util.List;

public class MainActivity extends FragmentActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Tournament tournament = new Tournament();
    private final CountdownTimer timer = new CountdownTimer();
    private static final long DOUBLE_PRESS_WINDOW_MS = 400L;
    private boolean waitingForSecondRewind;

    private final Runnable clearRewindWindowRunnable = new Runnable() {
        @Override
        public void run() {
            waitingForSecondRewind = false;
        }
    };

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerText();
            handler.postDelayed(this, 250);
        }
    };

    private TextView timerTextView;
    private TextView levelTextView;
    private TextView tournamentNameTextView;
    private TextView levelTitleTextView;
    private TextView blindsTextView;
    private TextView lastButtonTextView;
    private TextView previousLevelTextView;
    private TextView nextLevelTextView;
    private EditText tournamentNameEditor;
    private View mainMenu;
    private TournamentEditorController editorController;
    private TournamentStore tournamentStore;
    private boolean allowDpadMediaFallback;
    private boolean hasAlerted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tournamentStore = new TournamentStore(this);
        tournament.replaceWith(tournamentStore.loadCurrent());
        bindViews();
        configureMenus();

        editorController = new TournamentEditorController(
                this,
                tournament,
                this::handleTournamentChange);
        allowDpadMediaFallback = !"Amazon".equalsIgnoreCase(Build.MANUFACTURER);
        updateLevelText();
        updateStatusText();
        updateTimerText();
    }

    private void bindViews() {
        timerTextView = findViewById(R.id.timer_text);
        levelTextView = findViewById(R.id.level_text);
        tournamentNameTextView = findViewById(R.id.tournament_name_text);
        levelTitleTextView = findViewById(R.id.level_title_text);
        blindsTextView = findViewById(R.id.blinds_text);
        lastButtonTextView = findViewById(R.id.last_button);
        previousLevelTextView = findViewById(R.id.previous_level_text);
        nextLevelTextView = findViewById(R.id.next_level_text);
        tournamentNameEditor = findViewById(R.id.menu_tournament_name);
        mainMenu = findViewById(R.id.main_menu);
    }

    private void configureMenus() {
        findViewById(R.id.edit_tournament_button).setOnClickListener(view -> {
            openEditor();
        });
        findViewById(R.id.save_tournament_button).setOnClickListener(view -> {
            saveNamedTournament();
        });
        findViewById(R.id.load_tournament_button).setOnClickListener(view -> {
            showLoadTournamentDialog();
        });
        findViewById(R.id.reset_level_button).setOnClickListener(view -> {
            resetLevel();
            closeMainMenu();
        });
        findViewById(R.id.reset_tournament_button).setOnClickListener(view -> {
            resetTournament();
            closeMainMenu();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tickRunnable);
    }

    @Override

    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(clearRewindWindowRunnable);
        waitingForSecondRewind = false;
        timer.pause();
        handler.removeCallbacks(tickRunnable);
        updateStatusText();
        persistCurrentTournament();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        showLastKey(keyCode);

        if (RemoteKeys.isMenu(keyCode, allowDpadMediaFallback)) {
            handleMenuKey();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && isAnyMenuOpen()) {
            handleBackKey();
            return true;
        }

        if (isAnyMenuOpen()) {
            return super.onKeyDown(keyCode, event);
        }

        if (RemoteKeys.isRewind(keyCode, allowDpadMediaFallback)) {
            handleRewindPress(event);
            return true;
        }

        if (RemoteKeys.isForward(keyCode, allowDpadMediaFallback)) {
            handleForwardPress();
            return true;
        }

        if (RemoteKeys.isPause(keyCode, allowDpadMediaFallback)) {
            toggleTimer();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showLastKey(int keyCode) {
        lastButtonTextView.setText(getString(
                R.string.last_button_pressed,
                KeyEvent.keyCodeToString(keyCode),
                keyCode));
    }

    private boolean isAnyMenuOpen() {
        return mainMenu.getVisibility() == View.VISIBLE
                || editorController.isOpen();
    }

    private void handleMenuKey() {
        if (editorController.isOpen()) {
            editorController.closeAndApply();
            openMainMenu();
        } else if (mainMenu.getVisibility() == View.VISIBLE) {
            closeMainMenu();
        } else {
            openMainMenu();
        }
    }

    private void handleRewindPress(KeyEvent event) {
        if (event.getRepeatCount() > 0) {
            return;
        }

        if (waitingForSecondRewind) {
            handler.removeCallbacks(clearRewindWindowRunnable);
            waitingForSecondRewind = false;
            goToPreviousLevel();
            return;
        }

        resetLevel();
        waitingForSecondRewind = true;
        handler.postDelayed(
                clearRewindWindowRunnable,
                DOUBLE_PRESS_WINDOW_MS);
    }

    private void handleBackKey() {
        if (editorController.isOpen()) {
            editorController.closeAndApply();
            openMainMenu();
        } else {
            closeMainMenu();
        }
    }

    private void handleForwardPress() {
        advanceLevel();
    }

    private void openMainMenu() {
        mainMenu.setVisibility(View.VISIBLE);
        tournamentNameEditor.setText(tournament.getName());
        tournamentNameEditor.requestFocus();
    }

    private void closeMainMenu() {
        if (mainMenu.getVisibility() == View.VISIBLE) {
            applyTournamentName();
        }

        mainMenu.setVisibility(View.GONE);
    }

    private void applyTournamentName() {
        tournament.setName(tournamentNameEditor.getText().toString().trim());
        updateLevelText();
        persistCurrentTournament();
    }

    private void saveNamedTournament() {
        applyTournamentName();

        if (tournament.getName().isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.tournament_name_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        tournamentStore.saveNamed(tournament);
        tournamentStore.saveCurrent(tournament);
        Toast.makeText(
                this,
                R.string.tournament_saved,
                Toast.LENGTH_SHORT).show();
    }

    private void showLoadTournamentDialog() {
        List<String> savedNames = tournamentStore.getSavedNames();

        if (savedNames.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.no_saved_tournaments,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = savedNames.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_tournament)
                .setItems(names, (dialog, selectedIndex) -> {
                    loadNamedTournament(names[selectedIndex]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadNamedTournament(String name) {
        Tournament loadedTournament = tournamentStore.loadNamed(name);

        if (loadedTournament == null) {
            return;
        }

        tournament.replaceWith(loadedTournament);
        tournament.reset();
        timer.reset(false);
        hasAlerted = false;
        tournamentNameEditor.setText(tournament.getName());
        updateLevelText();
        updateTimerText();
        persistCurrentTournament();
        closeMainMenu();
    }

    private void persistCurrentTournament() {
        tournamentStore.saveCurrent(tournament);
    }

    private void openEditor() {
        closeMainMenu();
        editorController.open();
    }

    private void handleTournamentChange(boolean currentLevelChanged) {
        if (currentLevelChanged) {
            resetLevel();
        }

        updateLevelText();
        persistCurrentTournament();
    }

    private void toggleTimer() {
        if (isWaitingAtIndefiniteBreak()) {
            finishIndefiniteBreak();
            return;
        }

        if (timer.isRunning()) {
            timer.pause();
        } else {
            timer.start();
        }

        updateStatusText();
        updateTimerText();
    }

    private void resetLevel() {
        boolean resume = timer.isRunning();

        if (tournament.getCurrentLevel().isIndefiniteBreak()) {
            timer.finish(currentDurationMs());
        } else {
            timer.reset(resume);
        }

        hasAlerted = false;
        updateStatusText();
        updateTimerText();
    }

    private void resetTournament() {
        tournament.reset();
        timer.reset(false);
        hasAlerted = false;
        updateLevelText();
        updateStatusText();
        updateTimerText();
    }

    private long currentDurationMs() {
        return tournament.getCurrentLevel().getMinutes() * 60_000L;
    }

    private void updateLevelText() {
        TournamentLevel level = tournament.getCurrentLevel();
        boolean paused = !timer.isRunning();
        tournamentNameTextView.setText(tournament.getName().isEmpty()
                ? getString(R.string.app_name)
                : tournament.getName());

        if (level.getTitle().isEmpty()) {
            levelTitleTextView.setVisibility(View.GONE);
        } else {
            levelTitleTextView.setText(level.getTitle());
            levelTitleTextView.setVisibility(View.VISIBLE);
        }

        if (level.isBreak()) {
            int previousLevelNumber = tournament.getPlayableLevelNumber(
                    tournament.getCurrentLevelIndex());
            levelTextView.setText(paused
                    ? getString(R.string.level_number_paused, previousLevelNumber)
                    : getString(R.string.level_number, previousLevelNumber));
            blindsTextView.setText(R.string.default_break_title);
            blindsTextView.setVisibility(View.VISIBLE);
            updateAdjacentLevelPreviews();
            return;
        }

        int levelNumber = tournament.getPlayableLevelNumber(
                tournament.getCurrentLevelIndex());
        levelTextView.setText(paused
                ? getString(R.string.level_number_paused, levelNumber)
                : getString(R.string.level_number, levelNumber));
        blindsTextView.setText(getString(
                R.string.blinds_amount,
                level.getSmallBlind(),
                level.getBigBlind()));
        blindsTextView.setVisibility(View.VISIBLE);
        updateAdjacentLevelPreviews();
    }

    private void updateAdjacentLevelPreviews() {
        int currentIndex = tournament.getCurrentLevelIndex();
        updateLevelPreview(
                previousLevelTextView,
                currentIndex - 1,
                R.string.previous_item,
                true);
        updateLevelPreview(
                nextLevelTextView,
                currentIndex + 1,
                R.string.next_item,
                false);
    }

    private void updateLevelPreview(
            TextView previewView,
            int itemIndex,
            int labelResource,
            boolean previousPreview) {
        if (itemIndex < 0 || itemIndex >= tournament.size()) {
            previewView.setVisibility(View.GONE);
            return;
        }

        String itemText = adjacentItemText(itemIndex, previousPreview);
        previewView.setText(getString(labelResource, itemText));
        previewView.setVisibility(View.VISIBLE);
    }

    private String adjacentItemText(int itemIndex, boolean previousPreview) {
        TournamentLevel level = tournament.getLevel(itemIndex);

        if (!level.isBreak()) {
            String title = level.getTitle().isEmpty()
                    ? getString(
                            R.string.level_number,
                            tournament.getPlayableLevelNumber(itemIndex))
                    : level.getTitle();
            return getString(
                    R.string.preview_level,
                    title,
                    level.getSmallBlind(),
                    level.getBigBlind());
        }

        String breakTitle = level.getTitle().isEmpty()
                ? getString(R.string.default_break_title)
                : level.getTitle();

        if (previousPreview) {
            TournamentLevel previousPlayableLevel = tournament.getPreviousPlayableLevelBefore(itemIndex);

            if (previousPlayableLevel == null) {
                return getString(R.string.preview_break, breakTitle);
            }

            return getString(
                    R.string.preview_break_with_previous_blinds,
                    breakTitle,
                    previousPlayableLevel.getSmallBlind(),
                    previousPlayableLevel.getBigBlind());
        }

        TournamentLevel nextPlayableLevel = tournament.getNextPlayableLevelAfter(itemIndex);

        if (nextPlayableLevel == null) {
            return getString(R.string.preview_break, breakTitle);
        }

        return getString(
                R.string.preview_break_with_blinds,
                breakTitle,
                nextPlayableLevel.getSmallBlind(),
                nextPlayableLevel.getBigBlind());
    }

    private void updateStatusText() {
        updateLevelText();
    }

    private void updateTimerText() {
        long durationMs = currentDurationMs();

        if (tournament.getCurrentLevel().isIndefiniteBreak()) {
            timer.finish(durationMs);
            timerTextView.setText("00:00");
            return;
        }

        if (timer.isExpired(durationMs)) {
            finishCurrentLevel();
            return;
        }

        long remainingMs = timer.getRemainingMs(durationMs);
        long totalSeconds = (remainingMs + 999L) / 1000L;
        timerTextView.setText(String.format(
                "%02d:%02d",
                totalSeconds / 60L,
                totalSeconds % 60L));
    }

    private void goToPreviousLevel() {
        if (!tournament.goBack()) {
            resetLevel();
            return;
        }

        updateLevelText();
        resetLevel();
    }

    private void advanceLevel() {
        if (!tournament.advance()) {
            timer.finish(currentDurationMs());
            hasAlerted = true;
            timerTextView.setText("00:00");
            levelTextView.setText(R.string.timer_finished);
            blindsTextView.setVisibility(View.GONE);
            return;
        }

        updateLevelText();
        resetLevel();
    }

    private void finishCurrentLevel() {
        timerTextView.setText("00:00");

        if (!hasAlerted) {
            hasAlerted = true;
            playTimerAlert();
        }

        if (tournament.advance()) {
            updateLevelText();
            resetLevel();
        } else {
            timer.pause();
            levelTextView.setText(R.string.timer_finished);
            blindsTextView.setVisibility(View.GONE);
        }
    }

    private boolean isWaitingAtIndefiniteBreak() {
        return tournament.getCurrentLevel().isIndefiniteBreak()
                && !timer.isRunning();
    }

    private void finishIndefiniteBreak() {
        hasAlerted = true;
        playTimerAlert();

        if (tournament.advance()) {
            timer.reset(true);
            hasAlerted = false;
            updateLevelText();
            updateTimerText();
            persistCurrentTournament();
        } else {
            timer.finish(currentDurationMs());
            levelTextView.setText(R.string.timer_finished);
            blindsTextView.setVisibility(View.GONE);
        }
    }

    private void playTimerAlert() {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2_000);
        handler.postDelayed(tone::release, 2_100);
    }
}
