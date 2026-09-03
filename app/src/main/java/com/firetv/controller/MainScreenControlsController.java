package com.firetv.controller;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;

import com.firetv.R;

public class MainScreenControlsController {
    private static final long CENTER_BORDER_TIMEOUT_MS = 5_000L;
    private static final long CENTER_BORDER_FADE_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View previousControl;
    private final View currentControl;
    private final View nextControl;
    private final Runnable goToPrevious;
    private final Runnable toggleTimer;
    private final Runnable advanceLevel;
    private final Runnable openMenu;
    private View controlBeforeMenu;
    private ObjectAnimator centerBorderAnimator;
    private final Runnable fadeCenterBorderRunnable;

    private void fadeCenterBorder() {
        if (currentControl.hasFocus() && currentControl.getBackground() != null) {
            centerBorderAnimator = ObjectAnimator.ofInt(
                    currentControl.getBackground(),
                    "alpha",
                    255,
                    0);
            centerBorderAnimator.setDuration(CENTER_BORDER_FADE_MS);
            centerBorderAnimator.start();
        }
    }

    public MainScreenControlsController(
            View previousControl,
            View currentControl,
            View nextControl,
            Runnable goToPrevious,
            Runnable toggleTimer,
            Runnable advanceLevel,
            Runnable openMenu) {
        this.previousControl = previousControl;
        this.currentControl = currentControl;
        this.nextControl = nextControl;
        this.goToPrevious = goToPrevious;
        this.toggleTimer = toggleTimer;
        this.advanceLevel = advanceLevel;
        this.openMenu = openMenu;
        fadeCenterBorderRunnable = this::fadeCenterBorder;
        controlBeforeMenu = currentControl;
        configure();
    }

    private void configure() {
        View.OnKeyListener horizontalNavigationOnly = (view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (view == currentControl && isCenterActivityKey(keyCode)) {
                showCenterBorder();
            }

            if (view == nextControl && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                openMenu.run();
                return true;
            }

            return keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
        };

        previousControl.setOnClickListener(view -> {
            goToPrevious.run();
            retainFocus(previousControl);
        });
        currentControl.setOnClickListener(view -> {
            showCenterBorder();
            toggleTimer.run();
        });
        nextControl.setOnClickListener(view -> {
            advanceLevel.run();
            retainFocus(nextControl);
        });

        previousControl.setOnKeyListener(horizontalNavigationOnly);
        currentControl.setOnKeyListener(horizontalNavigationOnly);
        nextControl.setOnKeyListener(horizontalNavigationOnly);
        currentControl.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                showCenterBorder();
            }
        });

        previousControl.setNextFocusRightId(R.id.current_level_control);
        currentControl.setNextFocusLeftId(R.id.previous_level_text);
        currentControl.setNextFocusRightId(R.id.next_level_text);
        nextControl.setNextFocusLeftId(R.id.current_level_control);
    }

    public void requestInitialFocus() {
        currentControl.requestFocus();
    }

    public void rememberControlBeforeMenu(View focusedView) {
        if (isMainControl(focusedView)) {
            controlBeforeMenu = focusedView;
        }
    }

    public void restoreFocusAfterMenu() {
        retainFocus(controlBeforeMenu);
    }

    public void stop() {
        handler.removeCallbacks(fadeCenterBorderRunnable);

        if (centerBorderAnimator != null) {
            centerBorderAnimator.cancel();
        }
    }

    private boolean isCenterActivityKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER;
    }

    private boolean isMainControl(View view) {
        return view == previousControl || view == currentControl || view == nextControl;
    }

    private void showCenterBorder() {
        handler.removeCallbacks(fadeCenterBorderRunnable);

        if (centerBorderAnimator != null) {
            centerBorderAnimator.cancel();
        }

        if (currentControl.getBackground() != null) {
            currentControl.getBackground().mutate().setAlpha(255);
        }

        handler.postDelayed(fadeCenterBorderRunnable, CENTER_BORDER_TIMEOUT_MS);
    }

    private void retainFocus(View preferredControl) {
        if (preferredControl.getVisibility() == View.VISIBLE) {
            preferredControl.requestFocus();
        } else {
            currentControl.requestFocus();
        }
    }
}
