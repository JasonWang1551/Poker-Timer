package com.firetv.controller;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import com.firetv.R;
import com.firetv.model.Tournament;
import com.firetv.model.TournamentLevel;

import java.util.ArrayList;
import java.util.List;

public class TournamentEditorController {
    public interface Listener {
        void onTournamentChanged(boolean currentLevelChanged);
    }

    private final Activity activity;
    private final Tournament tournament;
    private final Listener listener;
    private final List<View> levelButtons = new ArrayList<>();
    private final View editMenu;
    private final TextView editorHeading;
    private final EditText editorTitle;
    private final EditText editorSmallBlind;
    private final EditText editorBigBlind;
    private final EditText editorMinutes;
    private final CheckBox doubleBigBlindCheckbox;
    private final CheckBox indefiniteBreakCheckbox;
    private final Button moveItemUpButton;
    private final Button moveItemDownButton;
    private final Button removeItemButton;
    private final View moveItemRow;
    private final LinearLayout levelListContainer;
    private final View smallBlindRow;
    private final View bigBlindRow;
    private final View timeRow;
    private int selectedIndex;
    private boolean populatingEditor;

    public TournamentEditorController(Activity activity, Tournament tournament, Listener listener) {
        this.activity = activity;
        this.tournament = tournament;
        this.listener = listener;
        editMenu = activity.findViewById(R.id.edit_menu);
        editorHeading = activity.findViewById(R.id.editor_heading);
        editorTitle = activity.findViewById(R.id.editor_title);
        editorSmallBlind = activity.findViewById(R.id.editor_small_blind);
        editorBigBlind = activity.findViewById(R.id.editor_big_blind);
        editorMinutes = activity.findViewById(R.id.editor_minutes);
        doubleBigBlindCheckbox = activity.findViewById(R.id.double_big_blind_checkbox);
        indefiniteBreakCheckbox = activity.findViewById(R.id.indefinite_break_checkbox);
        moveItemUpButton = activity.findViewById(R.id.move_item_up_button);
        moveItemDownButton = activity.findViewById(R.id.move_item_down_button);
        removeItemButton = activity.findViewById(R.id.remove_item_button);
        moveItemRow = activity.findViewById(R.id.move_item_row);
        levelListContainer = activity.findViewById(R.id.level_list_container);
        smallBlindRow = activity.findViewById(R.id.small_blind_row);
        bigBlindRow = activity.findViewById(R.id.big_blind_row);
        timeRow = activity.findViewById(R.id.time_row);
        configureNavigation();
    }

    private void configureNavigation() {
        View.OnKeyListener returnToLevelList = (view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                focusSelectedButton();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                return moveEditorFocusDown(view);
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                return moveEditorFocusUp(view);
            }

            return false;
        };

        editorTitle.setOnKeyListener(returnToLevelList);
        editorSmallBlind.setOnKeyListener(returnToLevelList);
        editorBigBlind.setOnKeyListener(returnToLevelList);
        editorMinutes.setOnKeyListener(returnToLevelList);
        doubleBigBlindCheckbox.setOnKeyListener(returnToLevelList);
        indefiniteBreakCheckbox.setOnKeyListener(returnToLevelList);
        moveItemUpButton.setOnKeyListener(returnToLevelList);
        moveItemDownButton.setOnKeyListener(returnToLevelList);
        removeItemButton.setOnKeyListener(returnToLevelList);
        moveItemUpButton.setOnClickListener(view -> {
            moveSelectedItem(-1);
        });
        moveItemDownButton.setOnClickListener(view -> {
            moveSelectedItem(1);
        });
        removeItemButton.setOnClickListener(view -> {
            removeSelectedItem();
        });
        doubleBigBlindCheckbox.setOnCheckedChangeListener((button, isChecked) -> {
            updateBigBlindEditorVisibility();

            if (!populatingEditor) {
                applySelectedEdits();
            }
        });
        indefiniteBreakCheckbox.setOnCheckedChangeListener((button, isChecked) -> {
            updateTimeEditorVisibility();

            if (!populatingEditor) {
                applySelectedEdits();
            }
        });
        addLiveUpdateListener(editorTitle);
        addLiveUpdateListener(editorSmallBlind);
        addLiveUpdateListener(editorBigBlind);
        addLiveUpdateListener(editorMinutes);
    }

    private boolean moveEditorFocusUp(View currentView) {
        TournamentLevel level = tournament.getLevel(selectedIndex);

        if (currentView == removeItemButton) {
            if (moveItemUpButton.isEnabled()) {
                moveItemUpButton.requestFocus();
            } else if (moveItemDownButton.isEnabled()) {
                moveItemDownButton.requestFocus();
            } else if (timeRow.getVisibility() == View.VISIBLE) {
                editorMinutes.requestFocus();
            } else {
                indefiniteBreakCheckbox.requestFocus();
            }

            return true;
        }

        if (currentView == moveItemDownButton || currentView == moveItemUpButton) {
            if (timeRow.getVisibility() == View.VISIBLE) {
                editorMinutes.requestFocus();
            } else {
                indefiniteBreakCheckbox.requestFocus();
            }

            return true;
        }

        if (currentView == editorMinutes) {
            if (bigBlindRow.getVisibility() == View.VISIBLE && !level.isBreak()) {
                editorBigBlind.requestFocus();
            } else if (!level.isBreak()) {
                doubleBigBlindCheckbox.requestFocus();
            } else {
                indefiniteBreakCheckbox.requestFocus();
            }
            return true;
        }

        if (currentView == indefiniteBreakCheckbox) {
            editorTitle.requestFocus();
            return true;
        }

        if (currentView == editorBigBlind) {
            doubleBigBlindCheckbox.requestFocus();
            return true;
        }

        if (currentView == doubleBigBlindCheckbox) {
            editorSmallBlind.requestFocus();
            return true;
        }

        if (currentView == editorSmallBlind) {
            editorTitle.requestFocus();
            return true;
        }

        return false;
    }

    private boolean moveEditorFocusDown(View currentView) {
        TournamentLevel level = tournament.getLevel(selectedIndex);

        if (currentView == editorTitle) {
            if (level.isBreak()) {
                indefiniteBreakCheckbox.requestFocus();
            } else {
                editorSmallBlind.requestFocus();
            }

            return true;
        }

        if (currentView == editorSmallBlind) {
            doubleBigBlindCheckbox.requestFocus();

            return true;
        }

        if (currentView == doubleBigBlindCheckbox) {
            if (bigBlindRow.getVisibility() == View.VISIBLE) {
                editorBigBlind.requestFocus();
            } else {
                editorMinutes.requestFocus();
            }

            return true;
        }

        if (currentView == editorBigBlind) {
            editorMinutes.requestFocus();
            return true;
        }

        if (currentView == indefiniteBreakCheckbox) {
            if (timeRow.getVisibility() == View.VISIBLE) {
                editorMinutes.requestFocus();
            } else {
                focusFirstAvailableAction();
            }

            return true;
        }

        if (currentView == editorMinutes) {
            focusFirstAvailableAction();
            return true;
        }

        if (currentView == moveItemDownButton || currentView == moveItemUpButton) {
            removeItemButton.requestFocus();
            return true;
        }

        return false;
    }

    private void focusFirstAvailableAction() {
        if (moveItemUpButton.isEnabled()) {
            moveItemUpButton.requestFocus();
        } else if (moveItemDownButton.isEnabled()) {
            moveItemDownButton.requestFocus();
        } else {
            removeItemButton.requestFocus();
        }
    }

    private void addLiveUpdateListener(EditText editor) {
        editor.addTextChangedListener(new TextWatcher() {
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
                if (!populatingEditor) {
                    applySelectedEdits();
                }
            }
        });
    }

    public void open() {
        editMenu.setVisibility(View.VISIBLE);
        selectedIndex = Math.min(tournament.getCurrentLevelIndex(), tournament.size() - 1);
        rebuildLevelList();
        showSelectedEditor();
        focusSelectedButton();
    }

    public void closeAndApply() {
        applySelectedEdits();
        editMenu.setVisibility(View.GONE);
    }

    public boolean isOpen() {
        return editMenu.getVisibility() == View.VISIBLE;
    }

    private void rebuildLevelList() {
        levelListContainer.removeAllViews();
        levelButtons.clear();

        for (int index = 0; index < tournament.size(); index++) {
            addSelectionButton(index);
        }

        addCreationRow();
    }

    private void addSelectionButton(int levelIndex) {
        TournamentLevel level = tournament.getLevel(levelIndex);
        View button = LayoutInflater.from(activity).inflate(
                R.layout.item_tournament_level,
                levelListContainer,
                false);
        updateLevelSelectionView(button, levelIndex, level);
        button.setLayoutParams(levelButtonLayoutParams());
        button.setOnClickListener(view -> {
            select(levelIndex);
            editorTitle.requestFocus();
        });
        button.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && isOpen()) {
                select(levelIndex);
            }
        });
        button.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                select(levelIndex);
                editorTitle.requestFocus();
                return true;
            }

            return false;
        });
        levelButtons.add(button);
        levelListContainer.addView(button);
    }

    private void addCreationRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(createAddButton(R.string.add_level, false));
        row.addView(createAddButton(R.string.add_break, true));
        levelListContainer.addView(row);
    }

    private Button createAddButton(int labelResource, boolean breakLevel) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(labelResource);
        button.setEnabled(!breakLevel || tournament.canAddBreak());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMarginEnd(dpToPixels(4));
        button.setLayoutParams(params);
        button.setOnClickListener(view -> {
            addItem(breakLevel);
        });
        return button;
    }

    private LinearLayout.LayoutParams levelButtonLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dpToPixels(8);
        return params;
    }

    private int dpToPixels(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void select(int levelIndex) {
        if (levelIndex == selectedIndex) {
            return;
        }

        applySelectedEdits();
        selectedIndex = levelIndex;
        showSelectedEditor();
    }

    private void addItem(boolean breakLevel) {
        applySelectedEdits();

        if (breakLevel) {
            selectedIndex = tournament.addBreak();
        } else {
            selectedIndex = tournament.addLevel();
        }

        rebuildLevelList();
        showSelectedEditor();
        focusSelectedButton();
        listener.onTournamentChanged(false);
    }

    private void removeSelectedItem() {
        if (tournament.size() == 1) {
            return;
        }

        TournamentLevel previousCurrentLevel = tournament.getCurrentLevel();
        selectedIndex = tournament.remove(selectedIndex);
        boolean currentLevelChanged = previousCurrentLevel
                != tournament.getCurrentLevel();
        rebuildLevelList();
        showSelectedEditor();
        focusSelectedButton();
        listener.onTournamentChanged(currentLevelChanged);
    }

    private void moveSelectedItem(int direction) {
        applySelectedEdits();
        selectedIndex = tournament.move(selectedIndex, selectedIndex + direction);
        rebuildLevelList();
        showSelectedEditor();
        focusSelectedButton();
        listener.onTournamentChanged(false);
    }

    private void showSelectedEditor() {
        TournamentLevel level = tournament.getLevel(selectedIndex);
        populatingEditor = true;

        try {
            editorHeading.setText(levelButtonText(selectedIndex, level));
            editorTitle.setText(level.getTitle());
            editorSmallBlind.setText(String.valueOf(level.getSmallBlind()));
            editorBigBlind.setText(String.valueOf(level.getBigBlind()));
            editorMinutes.setText(String.valueOf(level.getMinutes()));
            doubleBigBlindCheckbox.setChecked(level.isBigBlindDoubleSmallBlind());
            indefiniteBreakCheckbox.setChecked(level.isIndefiniteBreak());
        } finally {
            populatingEditor = false;
        }

        doubleBigBlindCheckbox.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        indefiniteBreakCheckbox.setVisibility(level.isBreak() ? View.VISIBLE : View.GONE);
        smallBlindRow.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        updateBigBlindEditorVisibility();
        updateTimeEditorVisibility();
        removeItemButton.setText(level.isBreak()
                ? R.string.remove_break
                : R.string.remove_level);
        moveItemRow.setVisibility(level.isBreak() ? View.VISIBLE : View.GONE);
        moveItemUpButton.setEnabled(tournament.canMove(selectedIndex, -1));
        moveItemDownButton.setEnabled(tournament.canMove(selectedIndex, 1));
        removeItemButton.setEnabled(tournament.canRemove(selectedIndex));
    }

    private void applySelectedEdits() {
        if (selectedIndex >= tournament.size()) {
            return;
        }

        TournamentLevel level = tournament.getLevel(selectedIndex);
        String title = editorTitle.getText().toString().trim();
        boolean indefiniteBreak = indefiniteBreakCheckbox.isChecked();
        int minutes = level.getMinutes();

        if (!indefiniteBreak) {
            minutes = positiveValue(editorMinutes, minutes);
        }

        boolean changed = !level.getTitle().equals(title)
                || level.isIndefiniteBreak() != indefiniteBreak
                || level.getMinutes() != minutes;
        boolean timingChanged = level.isIndefiniteBreak() != indefiniteBreak
                || level.getMinutes() != minutes;

        level.setTitle(title);
        level.setIndefiniteBreak(indefiniteBreak);

        if (!level.isIndefiniteBreak()) {
            level.setMinutes(minutes);
        }

        if (!level.isBreak()) {
            int smallBlind = positiveValue(editorSmallBlind, level.getSmallBlind());
            boolean doubleBigBlind = doubleBigBlindCheckbox.isChecked();
            int bigBlind;

            if (doubleBigBlind) {
                bigBlind = smallBlind * 2;
            } else {
                bigBlind = positiveValue(editorBigBlind, level.getBigBlind());
            }

            changed = changed
                    || level.getSmallBlind() != smallBlind
                    || level.getBigBlind() != bigBlind
                    || level.isBigBlindDoubleSmallBlind() != doubleBigBlind;
            level.setSmallBlind(smallBlind);
            level.setBigBlind(bigBlind);
            level.setBigBlindDoubleSmallBlind(doubleBigBlind);
        }

        if (!changed) {
            return;
        }

        if (selectedIndex < levelButtons.size()) {
            updateLevelSelectionView(
                    levelButtons.get(selectedIndex),
                    selectedIndex,
                    level);
        }

        boolean currentTimingChanged = timingChanged
                && selectedIndex == tournament.getCurrentLevelIndex();
        listener.onTournamentChanged(currentTimingChanged);
    }

    private int positiveValue(EditText editor, int fallback) {
        try {
            int value = Integer.parseInt(editor.getText().toString().trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void updateBigBlindEditorVisibility() {
        TournamentLevel level = tournament.getLevel(selectedIndex);
        boolean showBigBlindEditor = !level.isBreak()
                && !doubleBigBlindCheckbox.isChecked();
        bigBlindRow.setVisibility(showBigBlindEditor ? View.VISIBLE : View.GONE);
    }

    private void updateTimeEditorVisibility() {
        TournamentLevel level = tournament.getLevel(selectedIndex);
        boolean showTimeEditor = !level.isBreak()
                || !indefiniteBreakCheckbox.isChecked();
        timeRow.setVisibility(showTimeEditor ? View.VISIBLE : View.GONE);
    }

    private String levelButtonText(int index, TournamentLevel level) {
        if (level.isBreak()) {
            if (level.isIndefiniteBreak()) {
                return activity.getString(R.string.break_list_item_indefinite);
            }

            return activity.getString(
                    R.string.break_list_item,
                    level.getMinutes());
        }

        return activity.getString(
                R.string.level_list_item,
                tournament.getPlayableLevelNumber(index),
                level.getSmallBlind(),
                level.getBigBlind(),
                level.getMinutes());
    }

    private void updateLevelSelectionView(View row, int index, TournamentLevel level) {
        TextView numberView = row.findViewById(R.id.list_item_number);
        TextView titleView = row.findViewById(R.id.list_item_title);
        TextView blindsView = row.findViewById(R.id.list_item_blinds);
        TextView timeView = row.findViewById(R.id.list_item_time);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                blindsView,
                10,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP);

        String title = level.getTitle().trim();
        titleView.setText(title);
        titleView.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);

        if (level.isBreak()) {
            numberView.setText("");
            blindsView.setText(R.string.default_break_title);
        } else {
            numberView.setText(activity.getString(
                    R.string.level_list_number,
                    tournament.getPlayableLevelNumber(index)));
            blindsView.setText(activity.getString(
                    R.string.level_list_blinds,
                    level.getSmallBlind(),
                    level.getBigBlind()));
        }

        if (level.isIndefiniteBreak()) {
            timeView.setText(R.string.indefinite);
        } else {
            timeView.setText(activity.getString(
                    R.string.level_list_time,
                    level.getMinutes()));
        }
    }

    private void focusSelectedButton() {
        if (selectedIndex < levelButtons.size()) {
            levelButtons.get(selectedIndex).requestFocus();
        }
    }
}
