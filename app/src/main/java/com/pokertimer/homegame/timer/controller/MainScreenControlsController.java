package com.pokertimer.homegame.timer.controller;

import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import com.pokertimer.homegame.timer.R;

public class MainScreenControlsController {
    private final View previousButton;
    private final View resetButton;
    private final ImageButton pauseResumeButton;
    private final View nextButton;
    private final View menuButton;
    private final Runnable openMenu;
    private View controlBeforeMenu;

    public MainScreenControlsController(
            View previousButton,
            View resetButton,
            ImageButton pauseResumeButton,
            View nextButton,
            View menuButton,
            Runnable goToPrevious,
            Runnable resetLevel,
            Runnable toggleTimer,
            Runnable advanceLevel,
            Runnable openMenu) {
        this.previousButton = previousButton;
        this.resetButton = resetButton;
        this.pauseResumeButton = pauseResumeButton;
        this.nextButton = nextButton;
        this.menuButton = menuButton;
        this.openMenu = openMenu;
        controlBeforeMenu = pauseResumeButton;

        configureButton(previousButton, goToPrevious);
        configureButton(resetButton, resetLevel);
        configureButton(pauseResumeButton, toggleTimer);
        configureButton(nextButton, advanceLevel);
        configureButton(menuButton, openMenu);
        configureNavigation();
    }

    private void configureButton(View button, Runnable action) {
        button.setOnClickListener(view -> {
            action.run();
            if (button != menuButton) {
                retainFocus(button);
            }
        });
    }

    private void configureNavigation() {
        View.OnKeyListener listener = (view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return true;

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                moveLeftFrom(view);
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                moveRightFrom(view);
                return true;
            }

            return false;
        };

        previousButton.setOnKeyListener(listener);
        resetButton.setOnKeyListener(listener);
        pauseResumeButton.setOnKeyListener(listener);
        nextButton.setOnKeyListener(listener);
        menuButton.setOnKeyListener(listener);

        previousButton.setNextFocusLeftId(R.id.previous_level_button);
        previousButton.setNextFocusRightId(R.id.main_reset_level_button);
        resetButton.setNextFocusLeftId(R.id.previous_level_button);
        resetButton.setNextFocusRightId(R.id.pause_resume_button);
        pauseResumeButton.setNextFocusLeftId(R.id.main_reset_level_button);
        pauseResumeButton.setNextFocusRightId(R.id.next_level_button);
        nextButton.setNextFocusLeftId(R.id.pause_resume_button);
        nextButton.setNextFocusRightId(R.id.main_menu_button);
        menuButton.setNextFocusLeftId(R.id.next_level_button);
        menuButton.setNextFocusRightId(R.id.main_menu_button);
    }

    private void moveLeftFrom(View view) {
        if (view == resetButton) {
            retainFocus(previousButton.isEnabled() ? previousButton : resetButton);
        } else if (view == pauseResumeButton) {
            retainFocus(resetButton);
        } else if (view == nextButton) {
            retainFocus(pauseResumeButton);
        } else if (view == menuButton) {
            retainFocus(nextButton.isEnabled() ? nextButton : pauseResumeButton);
        }
    }

    private void moveRightFrom(View view) {
        if (view == previousButton) {
            retainFocus(resetButton);
        } else if (view == resetButton) {
            retainFocus(pauseResumeButton);
        } else if (view == pauseResumeButton) {
            retainFocus(nextButton.isEnabled() ? nextButton : menuButton);
        } else if (view == nextButton) {
            retainFocus(menuButton);
        } else if (view == menuButton) {
            openMenu.run();
        }
    }

    public void requestInitialFocus() {
        pauseResumeButton.requestFocus();
    }

    public void setTimerRunning(boolean running) {
        pauseResumeButton.setImageResource(running ? R.drawable.ic_pause : R.drawable.ic_play);
        pauseResumeButton.setContentDescription(pauseResumeButton.getContext().getString(
                running ? R.string.pause_timer : R.string.resume_timer));
    }

    public void setNavigationAvailability(boolean hasPrevious, boolean hasNext) {
        previousButton.setEnabled(hasPrevious);
        nextButton.setEnabled(hasNext);
        if ((!hasPrevious && previousButton.hasFocus()) || (!hasNext && nextButton.hasFocus())) {
            pauseResumeButton.requestFocus();
        }
    }

    public void rememberControlBeforeMenu(View focusedView) {
        if (isMainControl(focusedView)) controlBeforeMenu = focusedView;
    }

    public void restoreFocusAfterMenu() {
        retainFocus(controlBeforeMenu);
    }

    private boolean isMainControl(View view) {
        return view == previousButton || view == resetButton || view == pauseResumeButton
                || view == nextButton || view == menuButton;
    }

    private void retainFocus(View preferredControl) {
        if (preferredControl != null && preferredControl.getVisibility() == View.VISIBLE
                && preferredControl.isEnabled()) {
            preferredControl.requestFocus();
        } else {
            pauseResumeButton.requestFocus();
        }
    }
}
