package com.pokertimer.homegame.timer.controller;

import com.pokertimer.homegame.timer.ui.ButtonColors;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.pokertimer.homegame.timer.R;

import java.util.ArrayList;
import java.util.List;

/** Owns saved theme selection, settings buttons, and live color/cursor updates. */
public final class ThemeController {
    private static final String SETTINGS_PREFERENCES = "poker_timer_settings";
    private static final String COLOR_SCHEME_KEY = "color_scheme";

    // Register each theme here: style and display name. The Settings buttons are generated.
    private enum ColorScheme {
        BLACK(R.style.Theme_PokerTimer, R.string.black_scheme),
        DAY(R.style.Theme_PokerTimer_Day, R.string.day_scheme),
        FOREST(R.style.Theme_PokerTimer_Forest, R.string.forest_green_scheme),
        BLUE(R.style.Theme_PokerTimer_Blue, R.string.blue_scheme),
        VIOLET(R.style.Theme_PokerTimer_Violet, R.string.violet_scheme),
        RED(R.style.Theme_PokerTimer_Red, R.string.red_scheme);

        final int style;
        final int label;

        ColorScheme(int style, int label) {
            this.style = style;
            this.label = label;
        }
    }

    private final Activity activity;
    private final Runnable onEditingPanelsReplaced;
    private ColorScheme current;
    private LinearLayout themeOptions;
    private boolean themeOptionsExpanded;
    private final List<View> themeButtons = new ArrayList<>();

    public ThemeController(Activity activity, Runnable onEditingPanelsReplaced) {
        this.activity = activity;
        this.onEditingPanelsReplaced = onEditingPanelsReplaced;
        current = storedColorScheme();
    }

    /** Call before super.onCreate and before inflating any views. */
    public void applySavedTheme() {
        activity.setTheme(current.style);
    }

    /** Connect all theme choices in one place; adding a theme needs no Activity edits. */
    public void bindSettings() {
        themeOptions = activity.findViewById(R.id.theme_options_container);
        populateThemeButtons();
        View themeSelector = activity.findViewById(R.id.theme_selector_button);
        themeSelector.setOnClickListener(view -> {
            // Opening the accordion is a navigation action: keep focus on the
            // selector so the user can enter the list deliberately.
            setThemeOptionsExpanded(themeOptions.getVisibility() != View.VISIBLE, false);
        });
        // Left/right stay inside the Settings menu. The menu is closed with Back
        // or the explicit Back to Menu button, never by moving left.
        View back = activity.findViewById(R.id.settings_back_button);
        configureThemeGrid(back);
    }

    private void configureThemeGrid(View backButton) {
        View selector = activity.findViewById(R.id.theme_selector_button);
        for (int index = 0; index < themeButtons.size(); index++) {
            int column = index % 2;
            int rowAbove = index - 2;
            int rowBelow = index + 2;
            View left = column == 0 ? themeButtons.get(index) : themeButtons.get(index - 1);
            View right = column == 0 && index + 1 < themeButtons.size()
                    ? themeButtons.get(index + 1) : themeButtons.get(index);
            View up = rowAbove >= 0 ? themeButtons.get(rowAbove) : selector;
            View down = rowBelow < themeButtons.size() ? themeButtons.get(rowBelow) : backButton;
            setHorizontalNeighbors(left, right);
            setVerticalNeighbors(themeButtons.get(index), up, down);
        }

        backButton.setNextFocusUpId(R.id.theme_selector_button);
    }

    private void populateThemeButtons() {
        themeButtons.clear();
        themeOptions.removeAllViews();
        ColorScheme[] schemes = ColorScheme.values();
        float density = activity.getResources().getDisplayMetrics().density;

        for (int index = 0; index < schemes.length; index += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (index > 0) {
                rowParams.topMargin = Math.round(8 * density);
            }
            row.setLayoutParams(rowParams);

            for (int column = 0; column < 2 && index + column < schemes.length; column++) {
                ColorScheme scheme = schemes[index + column];
                Button button = new Button(activity);
                button.setId(View.generateViewId());
                button.setAllCaps(false);
                button.setText(scheme.label);
                button.setMinHeight(Math.round(40 * density));
                button.setPadding(Math.round(16 * density), Math.round(10 * density),
                        Math.round(16 * density), Math.round(10 * density));
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                if (column > 0) {
                    buttonParams.setMarginStart(Math.round(8 * density));
                }
                button.setLayoutParams(buttonParams);
                ButtonColors.apply(button);
                button.setOnClickListener(view -> select(scheme));
                themeButtons.add(button);
                row.addView(button);
            }
            themeOptions.addView(row);
        }
    }

    private void setHorizontalNeighbors(View left, View right) {
        left.setNextFocusLeftId(left.getId());
        left.setNextFocusRightId(right.getId());
        right.setNextFocusLeftId(left.getId());
        right.setNextFocusRightId(right.getId());
    }

    private void setVerticalNeighbors(View view, View up, View down) {
        view.setNextFocusUpId(up.getId());
        view.setNextFocusDownId(down.getId());
    }

    private void select(ColorScheme scheme) {
        current = scheme;
        activity.getSharedPreferences(SETTINGS_PREFERENCES, Activity.MODE_PRIVATE)
                .edit().putString(COLOR_SCHEME_KEY, scheme.name()).apply();
        activity.getTheme().applyStyle(scheme.style, true);
        refreshLegacyEditingPanels();
        refreshViews();
        setThemeOptionsExpanded(true, true);
    }

    private void setThemeOptionsExpanded(boolean expanded, boolean focusTheme) {
        if (themeOptions == null) {
            return;
        }
        themeOptionsExpanded = expanded;
        themeOptions.setVisibility(expanded ? View.VISIBLE : View.GONE);
        activity.findViewById(R.id.settings_back_button).setNextFocusUpId(
                expanded && !themeButtons.isEmpty()
                        ? themeButtons.get((themeButtons.size() - 1) / 2 * 2).getId()
                        : R.id.theme_selector_button);
        ((Button) activity.findViewById(R.id.theme_selector_button)).setText(
                activity.getString(expanded
                        ? R.string.choose_scheme_expanded
                        : R.string.choose_scheme_collapsed,
                        activity.getString(current.label)));
        if (expanded && focusTheme) {
            if (current.ordinal() < themeButtons.size()) {
                themeButtons.get(current.ordinal()).requestFocus();
            }
        }
    }

    private ColorScheme storedColorScheme() {
        SharedPreferences preferences = activity.getSharedPreferences(
                SETTINGS_PREFERENCES, Activity.MODE_PRIVATE);
        String stored = preferences.getString(COLOR_SCHEME_KEY, null);
        if (stored != null) {
            try {
                return ColorScheme.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
                return ColorScheme.BLACK;
            }
        }
        return ColorScheme.BLACK;
    }

    /** Call after the initial layout is inflated; also used after each theme change. */
    public void refreshViews() {
        setBackground(R.id.main_root, R.attr.timerBackground);
        setMenuBackground(R.id.main_menu);
        setMenuBackground(R.id.settings_menu);
        setMenuBackground(R.id.edit_menu);
        setBackground(R.id.editor_divider, R.attr.themeText);
        refreshThemeColors(activity.findViewById(R.id.main_root));

        ((Button) activity.findViewById(R.id.theme_selector_button)).setText(
                activity.getString(themeOptionsExpanded
                        ? R.string.choose_scheme_expanded
                        : R.string.choose_scheme_collapsed,
                        activity.getString(current.label)));
    }

    private void setBackground(int viewId, int colorAttribute) {
        activity.findViewById(viewId).setBackgroundColor(themeColor(colorAttribute));
    }

    private void setMenuBackground(int viewId) {
        activity.findViewById(viewId).setBackgroundColor(ColorUtils.setAlphaComponent(
                themeColor(R.attr.menuBackground), Math.round(menuOpacity() * 255)));
    }

    private float menuOpacity() {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.menuOpacity, value, true);
        return value.getFloat();
    }

    private int themeColor(int attribute) {
        return ButtonColors.themeColor(activity, attribute);
    }

    // Refresh existing controls without recreating the activity or pausing the timer.
    // Dynamically created editor buttons also call ButtonColors.apply.
    private void refreshThemeColors(View view) {
        // Text colors must also refresh when switching between light and dark themes.
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(themeColor(
                    view.getId() == R.id.level_text ? R.attr.themeAccent : R.attr.themeText));
        }
        if (view.getId() == R.id.list_item_divider) {
            view.setBackgroundColor(themeColor(R.attr.themeText));
        }
        if (view.getBackground() != null && view.getBackground().canApplyTheme()) {
            view.getBackground().mutate().applyTheme(activity.getTheme());
        }
        if ("level_selection".equals(view.getTag())) {
            ButtonColors.applySelection(view);
        }
        if (view instanceof CompoundButton || view instanceof EditText) {
            ColorStateList tint = new ColorStateList(new int[][]{
                    new int[]{-android.R.attr.state_enabled},
                    new int[]{android.R.attr.state_focused},
                    new int[]{android.R.attr.state_checked}, new int[]{}
            }, new int[]{ButtonColors.disabledColor(themeColor(R.attr.themeText)),
                    themeColor(R.attr.themeAccent), themeColor(R.attr.themeAccent),
                    themeColor(R.attr.themeText)});
            if (view instanceof CompoundButton) {
                ((CompoundButton) view).setButtonTintList(tint);
            } else {
                view.setBackgroundTintList(tint);
                refreshCursor((EditText) view);
            }
        } else if (view instanceof Button || view instanceof ImageButton) {
            ButtonColors.apply(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                refreshThemeColors(group.getChildAt(index));
            }
        }
    }

    private void refreshCursor(EditText editor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return; // Older versions receive fresh cursors when their panels are rebuilt.
        }
        boolean visible = editor.isCursorVisible();
        editor.setCursorVisible(false);
        // Build a fresh drawable from the resolved color. Reloading the XML resource
        // can reuse a cursor whose color was resolved under the previous theme.
        int width = Math.max(1, Math.round(2 * activity.getResources()
                .getDisplayMetrics().density));
        GradientDrawable cursor = new GradientDrawable();
        cursor.setSize(width, width);
        cursor.setColor(themeColor(R.attr.themeAccent));
        editor.setTextCursorDrawable(new InsetDrawable(cursor, width));
        editor.setCursorVisible(visible);
        editor.invalidate();
    }

    private void refreshLegacyEditingPanels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return;
        }
        // Called only from Settings, where both editing panels are hidden.
        // Keep the timer and Settings views alive while replacing cached cursors.
        replaceEditingPanel(R.id.main_menu, R.layout.view_tournament_menu);
        replaceEditingPanel(R.id.edit_menu, R.layout.view_tournament_editor);
        onEditingPanelsReplaced.run();
    }

    private void replaceEditingPanel(int viewId, int layoutId) {
        View oldPanel = activity.findViewById(viewId);
        ViewGroup parent = (ViewGroup) oldPanel.getParent();
        int index = parent.indexOfChild(oldPanel);
        SparseArray<Parcelable> state = new SparseArray<>();
        oldPanel.saveHierarchyState(state);
        // Give older text fields a new theme context as well as a new view, so
        // framework cursor loading cannot keep using their original theme context.
        ContextThemeWrapper context = new ContextThemeWrapper(activity, current.style);
        View replacement = LayoutInflater.from(activity).cloneInContext(context)
                .inflate(layoutId, parent, false);
        // Restore before attaching listeners, so restoring text doesn't edit the model.
        replacement.restoreHierarchyState(state);
        replacement.setVisibility(oldPanel.getVisibility());
        parent.removeViewAt(index);
        parent.addView(replacement, index, oldPanel.getLayoutParams());
    }

}
