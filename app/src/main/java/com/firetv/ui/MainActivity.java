package com.firetv.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.firetv.R;
import com.firetv.controller.MainScreenControlsController;
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
    private static final long DOUBLE_PRESS_WINDOW_MS = 1000L;
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
    private TextView previousLevelTextView;
    private TextView nextLevelTextView;
    private EditText tournamentNameEditor;
    private View mainMenu;
    private View editTournamentButton;
    private MainScreenControlsController mainScreenControls;
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
        mainScreenControls = new MainScreenControlsController(
                previousLevelTextView,
                findViewById(R.id.current_level_control),
                nextLevelTextView,
                this::goToPreviousLevel,
                this::toggleTimer,
                this::advanceLevel,
                this::openMainMenu);
        configureMenus();

        editorController = new TournamentEditorController(
                this,
                tournament,
                this::handleTournamentChange,
                this::returnFromEditorToMainMenu);
        allowDpadMediaFallback = !"Amazon".equalsIgnoreCase(Build.MANUFACTURER);
        updateLevelText();
        updateStatusText();
        updateTimerText();
        mainScreenControls.requestInitialFocus();
    }

    private void bindViews() {
        timerTextView = findViewById(R.id.timer_text);
        levelTextView = findViewById(R.id.level_text);
        tournamentNameTextView = findViewById(R.id.tournament_name_text);
        levelTitleTextView = findViewById(R.id.level_title_text);
        blindsTextView = findViewById(R.id.blinds_text);
        previousLevelTextView = findViewById(R.id.previous_level_text);
        nextLevelTextView = findViewById(R.id.next_level_text);
        tournamentNameEditor = findViewById(R.id.menu_tournament_name);
        mainMenu = findViewById(R.id.main_menu);
        editTournamentButton = findViewById(R.id.edit_tournament_button);
    }

    private void configureMenus() {
        tournamentNameEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                tournament.setName(editable.toString().trim());
                updateLevelText();
                persistCurrentTournament();
            }
        });

        editTournamentButton.setOnClickListener(view -> {
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

        View.OnKeyListener closeMenuOnLeft = (view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (view.getId() == R.id.load_tournament_button
                    && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                findViewById(R.id.save_tournament_button).requestFocus();
                return true;
            }

            if (view.getId() == R.id.save_tournament_button
                    && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                findViewById(R.id.load_tournament_button).requestFocus();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                closeMainMenu();
                return true;
            }

            if (view.getId() == R.id.reset_tournament_button
                    && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                return true;
            }

            if (view == tournamentNameEditor
                    && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                return true;
            }

            return false;
        };
        tournamentNameEditor.setOnKeyListener(closeMenuOnLeft);
        findViewById(R.id.save_tournament_button).setOnKeyListener(closeMenuOnLeft);
        findViewById(R.id.load_tournament_button).setOnKeyListener(closeMenuOnLeft);
        editTournamentButton.setOnKeyListener(closeMenuOnLeft);
        findViewById(R.id.reset_level_button).setOnKeyListener(closeMenuOnLeft);
        findViewById(R.id.reset_tournament_button).setOnKeyListener(closeMenuOnLeft);
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
        mainScreenControls.stop();

        waitingForSecondRewind = false;
        timer.pause();
        handler.removeCallbacks(tickRunnable);
        updateStatusText();
        persistCurrentTournament();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (RemoteKeys.isMenu(keyCode, allowDpadMediaFallback)) {
            handleMenuKey();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && isAnyMenuOpen()) {
            handleBackKey();
            return true;
        }

        if (isAnyMenuOpen()) {
            if (wouldLeaveOpenMenu(keyCode)) {
                return true;
            }

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

    private boolean wouldLeaveOpenMenu(int keyCode) {
        int focusDirection;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                focusDirection = View.FOCUS_LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                focusDirection = View.FOCUS_RIGHT;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                focusDirection = View.FOCUS_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                focusDirection = View.FOCUS_DOWN;
                break;
            default:
                return false;
        }

        View focusedView = getCurrentFocus();

        if (focusedView == null) {
            return true;
        }

        View nextFocus = focusedView.focusSearch(focusDirection);
        View openMenu = editorController.isOpen()
                ? findViewById(R.id.edit_menu)
                : mainMenu;
        return nextFocus == null || !isDescendantOf(nextFocus, openMenu);
    }

    private boolean isDescendantOf(View view, View ancestor) {
        ViewParent parent = view.getParent();

        while (parent != null) {
            if (parent == ancestor) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
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
        mainScreenControls.rememberControlBeforeMenu(getCurrentFocus());

        mainMenu.setVisibility(View.VISIBLE);
        tournamentNameEditor.setText(tournament.getName());
        tournamentNameEditor.requestFocus();
    }

    private void closeMainMenu() {
        if (mainMenu.getVisibility() == View.VISIBLE) {
            applyTournamentName();
        }

        mainMenu.setVisibility(View.GONE);
        mainScreenControls.restoreFocusAfterMenu();
    }

    private void returnFromEditorToMainMenu() {
        openMainMenu();
        editTournamentButton.requestFocus();
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

        if (!tournamentStore.isSaved(tournament)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.unsaved_tournament)
                    .setMessage(R.string.confirm_load_over_unsaved_tournament)
                    .setPositiveButton(
                            R.string.continue_loading,
                            (dialog, button) -> showLoadTournamentChoices(savedNames))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        showLoadTournamentChoices(savedNames);
    }

    private void showLoadTournamentChoices(List<String> savedNames) {
        String[] names = savedNames.toArray(new String[0]);
        int[] selectedIndex = {savedNames.indexOf(tournament.getName())};
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_tournament)
                .setSingleChoiceItems(names, selectedIndex[0], (dialog, index) -> {
                    selectedIndex[0] = index;
                })
                .setPositiveButton(R.string.load_tournament, (dialog, button) -> {
                    if (selectedIndex[0] >= 0) {
                        loadNamedTournament(names[selectedIndex[0]]);
                    } else {
                        Toast.makeText(
                                this,
                                R.string.choose_tournament_first,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.delete_tournament, (dialog, button) -> {
                    if (selectedIndex[0] >= 0) {
                        confirmDeleteTournament(names[selectedIndex[0]]);
                    } else {
                        Toast.makeText(
                                this,
                                R.string.choose_tournament_first,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmDeleteTournament(String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_tournament)
                .setMessage(getString(R.string.confirm_delete_tournament, name))
                .setPositiveButton(R.string.delete_tournament, (dialog, button) -> {
                    tournamentStore.deleteNamed(name);
                    Toast.makeText(
                            this,
                            R.string.tournament_deleted,
                            Toast.LENGTH_SHORT).show();
                    List<String> remainingNames = tournamentStore.getSavedNames();

                    if (remainingNames.isEmpty()) {
                        Toast.makeText(
                                this,
                                R.string.no_saved_tournaments,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showLoadTournamentChoices(remainingNames);
                    }
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
        return tournament.getEffectiveMinutes(
                tournament.getCurrentLevelIndex()) * 60_000L;
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
        blindsTextView.setText(mainBlindsText(level));
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
            return previewLevelText(title, level);
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
                    previousPlayableLevel.hasAnte()
                            ? R.string.preview_break_with_previous_blinds_and_ante
                            : R.string.preview_break_with_previous_blinds,
                    breakTitle,
                    previousPlayableLevel.getSmallBlind(),
                    previousPlayableLevel.getBigBlind(),
                    previousPlayableLevel.getAnte());
        }

        TournamentLevel nextPlayableLevel = tournament.getNextPlayableLevelAfter(itemIndex);

        if (nextPlayableLevel == null) {
            return getString(R.string.preview_break, breakTitle);
        }

        return getString(
                nextPlayableLevel.hasAnte()
                        ? R.string.preview_break_with_blinds_and_ante
                        : R.string.preview_break_with_blinds,
                breakTitle,
                nextPlayableLevel.getSmallBlind(),
                nextPlayableLevel.getBigBlind(),
                nextPlayableLevel.getAnte());
    }

    private String mainBlindsText(TournamentLevel level) {
        if (level.hasAnte()) {
            return getString(
                    R.string.blinds_amount_with_ante,
                    level.getSmallBlind(),
                    level.getBigBlind(),
                    level.getAnte());
        }

        return getString(
                R.string.blinds_amount,
                level.getSmallBlind(),
                level.getBigBlind());
    }

    private String previewLevelText(String title, TournamentLevel level) {
        if (level.hasAnte()) {
            return getString(
                    R.string.preview_level_with_ante,
                    title,
                    level.getSmallBlind(),
                    level.getBigBlind(),
                    level.getAnte());
        }

        return getString(
                R.string.preview_level,
                title,
                level.getSmallBlind(),
                level.getBigBlind());
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
