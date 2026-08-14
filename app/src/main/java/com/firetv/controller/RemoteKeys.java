package com.firetv.controller;

import android.view.KeyEvent;

public final class RemoteKeys {
    private RemoteKeys() {
    }

    public static boolean isMenu(int keyCode, boolean allowDpadFallback) {
        return keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_INFO
                || (allowDpadFallback && keyCode == KeyEvent.KEYCODE_TAB);
    }

    public static boolean isPause(int keyCode, boolean allowDpadFallback) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || (allowDpadFallback && keyCode == KeyEvent.KEYCODE_DPAD_CENTER);
    }

    public static boolean isRewind(int keyCode, boolean allowDpadFallback) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || (allowDpadFallback
                && keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
    }

    public static boolean isForward(int keyCode, boolean allowDpadFallback) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || (allowDpadFallback
                && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
    }
}
