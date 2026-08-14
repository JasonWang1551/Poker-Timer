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
    private final CheckBox setLevelTimeCheckbox;
    private final CheckBox hasAnteCheckbox;
    private final CheckBox anteEqualsBigBlindCheckbox;
    private final EditText editorAnte;
    private final Button moveItemUpButton;
    private final Button moveItemDownButton;
    private final Button removeItemButton;
    private final View moveItemRow;
    private final LinearLayout levelListContainer;
    private final View smallBlindRow;
    private final View bigBlindRow;
    private final View timeRow;
    private final View anteRow;
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
        setLevelTimeCheckbox = activity.findViewById(R.id.set_level_time_checkbox);
        hasAnteCheckbox = activity.findViewById(R.id.has_ante_checkbox);
        anteEqualsBigBlindCheckbox = activity.findViewById(R.id.ante_equals_big_blind_checkbox);
        editorAnte = activity.findViewById(R.id.editor_ante);
        moveItemUpButton = activity.findViewById(R.id.move_item_up_button);
        moveItemDownButton = activity.findViewById(R.id.move_item_down_button);
        removeItemButton = activity.findViewById(R.id.remove_item_button);
        moveItemRow = activity.findViewById(R.id.move_item_row);
        levelListContainer = activity.findViewById(R.id.level_list_container);
        smallBlindRow = activity.findViewById(R.id.small_blind_row);
        bigBlindRow = activity.findViewById(R.id.big_blind_row);
        timeRow = activity.findViewById(R.id.time_row);
        anteRow = activity.findViewById(R.id.ante_row);
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
        setLevelTimeCheckbox.setOnKeyListener(returnToLevelList);
        hasAnteCheckbox.setOnKeyListener(returnToLevelList);
        anteEqualsBigBlindCheckbox.setOnKeyListener(returnToLevelList);
        editorAnte.setOnKeyListener(returnToLevelList);
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
        setLevelTimeCheckbox.setOnCheckedChangeListener((button, isChecked) -> {
            updateTimeEditorVisibility();

            if (!populatingEditor) {
                applySelectedEdits();
            }
        });
        hasAnteCheckbox.setOnCheckedChangeListener((button, isChecked) -> {
            updateAnteEditorVisibility();

            if (!populatingEditor) {
                applySelectedEdits();
            }
        });
        anteEqualsBigBlindCheckbox.setOnCheckedChangeListener((button, isChecked) -> {
            updateAnteEditorVisibility();

            if (!populatingEditor) {
                applySelectedEdits();
            }
        });
        addLiveUpdateListener(editorTitle);
        addLiveUpdateListener(editorSmallBlind);
        addLiveUpdateListener(editorBigBlind);
        addLiveUpdateListener(editorMinutes);
        addLiveUpdateListener(editorAnte);
    }

    private boolean moveEditorFocusUp(View currentView) {
        if (currentView == removeItemButton) {
            if (moveItemUpButton.isEnabled()) {
                moveItemUpButton.requestFocus();
            } else if (moveItemDownButton.isEnabled()) {
                moveItemDownButton.requestFocus();
            } else if (timeRow.getVisibility() == View.VISIBLE) {
                focusLastEditorField();
            } else {
                focusLastEditorField();
            }

            return true;
        }

        if (currentView == moveItemDownButton || currentView == moveItemUpButton) {
            focusLastEditorField();
            return true;
        }

        return moveWithinEditorFields(currentView, -1);
    }

    private boolean moveEditorFocusDown(View currentView) {
        if (currentView == moveItemDownButton || currentView == moveItemUpButton) {
            removeItemButton.requestFocus();
            return true;
        }

        if (moveWithinEditorFields(currentView, 1)) {
            return true;
        }

        if (isEditorField(currentView)) {
            focusFirstAvailableAction();
            return true;
        }

        return false;
    }

    private boolean moveWithinEditorFields(View currentView, int direction) {
        List<View> fields = editorFocusOrder();
        int currentIndex = fields.indexOf(currentView);
        int targetIndex = currentIndex + direction;

        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= fields.size()) {
            return false;
        }

        fields.get(targetIndex).requestFocus();
        return true;
    }

    private boolean isEditorField(View view) {
        return editorFocusOrder().contains(view);
    }

    private void focusLastEditorField() {
        List<View> fields = editorFocusOrder();

        if (!fields.isEmpty()) {
            fields.get(fields.size() - 1).requestFocus();
        }
    }

    private List<View> editorFocusOrder() {
        List<View> fields = new ArrayList<>();
        TournamentLevel level = tournament.getLevel(selectedIndex);
        fields.add(editorTitle);

        if (level.isBreak()) {
            fields.add(indefiniteBreakCheckbox);

            if (!indefiniteBreakCheckbox.isChecked()) {
                fields.add(setLevelTimeCheckbox);

                if (setLevelTimeCheckbox.isChecked()) {
                    fields.add(editorMinutes);
                }
            }

            return fields;
        }

        fields.add(editorSmallBlind);
        fields.add(doubleBigBlindCheckbox);

        if (!doubleBigBlindCheckbox.isChecked()) {
            fields.add(editorBigBlind);
        }

        fields.add(hasAnteCheckbox);

        if (hasAnteCheckbox.isChecked()) {
            fields.add(anteEqualsBigBlindCheckbox);

            if (!anteEqualsBigBlindCheckbox.isChecked()) {
                fields.add(editorAnte);
            }
        }

        if (!tournament.isFirstPlayableLevel(selectedIndex)) {
            fields.add(setLevelTimeCheckbox);
        }

        if (tournament.isFirstPlayableLevel(selectedIndex)
                || setLevelTimeCheckbox.isChecked()) {
            fields.add(editorMinutes);
        }

        return fields;
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
        button.setId(View.generateViewId());
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
        Button addLevelButton = createAddButton(R.string.add_level, false);
        Button addBreakButton = createAddButton(R.string.add_break, true);
        addLevelButton.setId(View.generateViewId());
        addBreakButton.setId(View.generateViewId());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(addLevelButton);
        row.addView(addBreakButton);
        levelListContainer.addView(row);

        if (!levelButtons.isEmpty()) {
            View finalLevelButton = levelButtons.get(levelButtons.size() - 1);
            finalLevelButton.setNextFocusDownId(addLevelButton.getId());
            addLevelButton.setNextFocusUpId(finalLevelButton.getId());
            addBreakButton.setNextFocusUpId(finalLevelButton.getId());
        }
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
            editorMinutes.setText(String.valueOf(
                    tournament.getEffectiveMinutes(selectedIndex)));
            editorAnte.setText(String.valueOf(level.getCustomAnte()));
            doubleBigBlindCheckbox.setChecked(level.isBigBlindDoubleSmallBlind());
            indefiniteBreakCheckbox.setChecked(level.isIndefiniteBreak());
            setLevelTimeCheckbox.setChecked(level.hasOwnTime());
            hasAnteCheckbox.setChecked(level.hasAnte());
            anteEqualsBigBlindCheckbox.setChecked(level.isAnteEqualToBigBlind());
        } finally {
            populatingEditor = false;
        }

        doubleBigBlindCheckbox.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        indefiniteBreakCheckbox.setVisibility(level.isBreak() ? View.VISIBLE : View.GONE);
        setLevelTimeCheckbox.setEnabled(
                !tournament.isFirstPlayableLevel(selectedIndex));
        hasAnteCheckbox.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        anteEqualsBigBlindCheckbox.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        smallBlindRow.setVisibility(level.isBreak() ? View.GONE : View.VISIBLE);
        updateBigBlindEditorVisibility();
        updateAnteEditorVisibility();
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
        int oldCurrentMinutes = tournament.getEffectiveMinutes(
                tournament.getCurrentLevelIndex());
        boolean oldCurrentIndefinite = tournament.getCurrentLevel()
                .isIndefiniteBreak();
        String title = editorTitle.getText().toString().trim();
        boolean indefiniteBreak = indefiniteBreakCheckbox.isChecked();
        boolean hasOwnTime = tournament.isFirstPlayableLevel(selectedIndex)
                || setLevelTimeCheckbox.isChecked();
        boolean hasAnte = hasAnteCheckbox.isChecked();
        boolean anteEqualsBigBlind = hasAnte
                && anteEqualsBigBlindCheckbox.isChecked();
        int minutes = level.getMinutes();

        if (!indefiniteBreak && hasOwnTime) {
            minutes = positiveValue(editorMinutes, minutes);
        }

        int ante = level.getCustomAnte();

        if (hasAnte && !anteEqualsBigBlind) {
            int fallbackAnte = ante > 0 ? ante : level.getBigBlind();
            ante = positiveValue(editorAnte, fallbackAnte);
        }

        boolean changed = !level.getTitle().equals(title)
                || level.isIndefiniteBreak() != indefiniteBreak
                || level.hasOwnTime() != hasOwnTime
                || level.getMinutes() != minutes
                || level.hasAnte() != hasAnte
                || level.isAnteEqualToBigBlind() != anteEqualsBigBlind
                || level.getCustomAnte() != ante;

        level.setTitle(title);
        level.setIndefiniteBreak(indefiniteBreak);
        level.setHasOwnTime(hasOwnTime);
        level.setHasAnte(hasAnte);
        level.setAnteEqualsBigBlind(anteEqualsBigBlind);
        level.setAnte(ante);

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

        updateAllLevelSelectionViews();

        boolean currentTimingChanged = oldCurrentMinutes
                != tournament.getEffectiveMinutes(tournament.getCurrentLevelIndex())
                || oldCurrentIndefinite != tournament.getCurrentLevel()
                .isIndefiniteBreak();
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
        boolean showTimeEditor;

        if (level.isBreak()) {
            boolean timedBreak = !indefiniteBreakCheckbox.isChecked();
            setLevelTimeCheckbox.setVisibility(
                    timedBreak ? View.VISIBLE : View.GONE);
            showTimeEditor = timedBreak && setLevelTimeCheckbox.isChecked();
        } else {
            setLevelTimeCheckbox.setVisibility(View.VISIBLE);
            showTimeEditor = tournament.isFirstPlayableLevel(selectedIndex)
                    || setLevelTimeCheckbox.isChecked();
        }

        timeRow.setVisibility(showTimeEditor ? View.VISIBLE : View.GONE);
    }

    private void updateAnteEditorVisibility() {
        TournamentLevel level = tournament.getLevel(selectedIndex);
        boolean showAnteOptions = !level.isBreak() && hasAnteCheckbox.isChecked();
        anteEqualsBigBlindCheckbox.setVisibility(
                showAnteOptions ? View.VISIBLE : View.GONE);
        boolean showCustomAnte = showAnteOptions
                && !anteEqualsBigBlindCheckbox.isChecked();
        anteRow.setVisibility(showCustomAnte ? View.VISIBLE : View.GONE);
    }

    private void updateAllLevelSelectionViews() {
        int rowCount = Math.min(levelButtons.size(), tournament.size());

        for (int index = 0; index < rowCount; index++) {
            updateLevelSelectionView(
                    levelButtons.get(index),
                    index,
                    tournament.getLevel(index));
        }
    }

    private String levelButtonText(int index, TournamentLevel level) {
        if (level.isBreak()) {
            if (level.isIndefiniteBreak()) {
                return activity.getString(R.string.break_list_item_indefinite);
            }

            return activity.getString(
                    R.string.break_list_item,
                    tournament.getEffectiveMinutes(index));
        }

        return activity.getString(
                R.string.level_list_item,
                tournament.getPlayableLevelNumber(index),
                level.getSmallBlind(),
                level.getBigBlind(),
                tournament.getEffectiveMinutes(index));
    }

    private void updateLevelSelectionView(View row, int index, TournamentLevel level) {
        TextView numberView = row.findViewById(R.id.list_item_number);
        TextView titleView = row.findViewById(R.id.list_item_title);
        TextView blindsView = row.findViewById(R.id.list_item_blinds);
        TextView anteView = row.findViewById(R.id.list_item_ante);
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
            anteView.setVisibility(View.GONE);
        } else {
            numberView.setText(activity.getString(
                    R.string.level_list_number,
                    tournament.getPlayableLevelNumber(index)));
            blindsView.setText(activity.getString(
                    R.string.level_list_blinds,
                    level.getSmallBlind(),
                    level.getBigBlind()));

            if (level.hasAnte()) {
                anteView.setText(activity.getString(
                        R.string.ante_display,
                        level.getAnte()));
                anteView.setVisibility(View.VISIBLE);
            } else {
                anteView.setVisibility(View.GONE);
            }
        }

        if (level.isIndefiniteBreak()) {
            timeView.setText(R.string.indefinite);
        } else {
            timeView.setText(activity.getString(
                    R.string.level_list_time,
                    tournament.getEffectiveMinutes(index)));
        }
    }

    private void focusSelectedButton() {
        if (selectedIndex < levelButtons.size()) {
            levelButtons.get(selectedIndex).requestFocus();
        }
    }
}
