package com.pokertimer.homegame.timer.controller;

import com.pokertimer.homegame.timer.ui.ButtonColors;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.pokertimer.homegame.timer.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Owns the alarm-sound list and the sound selected in Settings. */
public final class AlarmSoundController {
    private static final String SETTINGS_PREFERENCES = "poker_timer_settings";
    private static final String ALARM_SOUND_KEY = "alarm_sound";

    private final Activity activity;
    private final List<AlarmSound> sounds = new ArrayList<>();
    private final List<View> soundButtons = new ArrayList<>();
    private final List<View> sampleButtons = new ArrayList<>();
    private LinearLayout soundOptions;
    private AlarmSound current;
    private MediaPlayer activePlayer;
    private int nextSectionId;

    public AlarmSoundController(Activity activity) {
        this.activity = activity;
        loadSounds();
    }

    /** Builds the selector from every audio file in res/raw. */
    public void bindSettings(int nextSectionId) {
        this.nextSectionId = nextSectionId;
        soundOptions = activity.findViewById(R.id.alarm_sound_options_container);
        populateSoundButtons();

        Button selector = activity.findViewById(R.id.alarm_sound_setting);
        selector.setOnClickListener(view -> setOptionsExpanded(
                soundOptions.getVisibility() != View.VISIBLE, false));
        selector.setNextFocusLeftId(selector.getId());
        selector.setNextFocusRightId(selector.getId());
        selector.setNextFocusUpId(selector.getId());
        setOptionsExpanded(false, false);
    }

    public void focusSelector() {
        activity.findViewById(R.id.alarm_sound_setting).requestFocus();
    }

    /** Plays the selected sound, then runs the supplied work after it finishes. */
    public boolean play(Runnable afterPlayback) {
        if (current == null) {
            runAfterPlayback(afterPlayback);
            return false;
        }

        return play(current, afterPlayback);
    }

    private boolean play(AlarmSound sound, Runnable afterPlayback) {
        releaseActivePlayer();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        MediaPlayer player = MediaPlayer.create(activity, sound.resourceId, attributes,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (player == null) {
            runAfterPlayback(afterPlayback);
            return false;
        }
        activePlayer = player;
        player.setOnCompletionListener(completedPlayer -> {
            releasePlayer(completedPlayer);
            runAfterPlayback(afterPlayback);
        });
        player.setOnErrorListener((failedPlayer, what, extra) -> {
            releasePlayer(failedPlayer);
            runAfterPlayback(afterPlayback);
            return true;
        });
        player.start();
        return true;
    }

    private void loadSounds() {
        Field[] rawResources = R.raw.class.getFields();
        Arrays.sort(rawResources, (left, right) -> left.getName().compareTo(right.getName()));
        for (Field resource : rawResources) {
            try {
                sounds.add(new AlarmSound(resource.getInt(null), resource.getName()));
            } catch (IllegalAccessException ignored) {
                // Public generated resource fields are accessible; ignore an unexpected one.
            }
        }

        if (sounds.isEmpty()) {
            return;
        }
        SharedPreferences preferences = activity.getSharedPreferences(
                SETTINGS_PREFERENCES, Activity.MODE_PRIVATE);
        String storedName = preferences.getString(ALARM_SOUND_KEY, null);
        current = sounds.get(0);
        for (AlarmSound sound : sounds) {
            if ("tannoy".equals(sound.resourceName)) {
                current = sound;
                break;
            }
        }
        for (AlarmSound sound : sounds) {
            if (sound.resourceName.equals(storedName)) {
                current = sound;
                break;
            }
        }
    }

    private void populateSoundButtons() {
        soundButtons.clear();
        sampleButtons.clear();
        soundOptions.removeAllViews();
        float density = activity.getResources().getDisplayMetrics().density;

        for (AlarmSound sound : sounds) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (!soundButtons.isEmpty()) {
                rowParams.topMargin = Math.round(8 * density);
            }
            row.setLayoutParams(rowParams);

            Button button = new Button(activity);
            button.setId(View.generateViewId());
            button.setAllCaps(false);
            button.setText(displayName(sound.resourceName));
            button.setMinHeight(Math.round(40 * density));
            button.setPadding(Math.round(16 * density), Math.round(10 * density),
                    Math.round(16 * density), Math.round(10 * density));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            button.setLayoutParams(params);
            ButtonColors.apply(button);
            button.setOnClickListener(view -> select(sound));

            ImageButton sampleButton = new ImageButton(activity);
            sampleButton.setId(View.generateViewId());
            sampleButton.setContentDescription(activity.getString(R.string.sample_sound));
            sampleButton.setImageResource(R.drawable.ic_play);
            sampleButton.setScaleType(ImageButton.ScaleType.CENTER);
            sampleButton.setMinimumWidth(Math.round(48 * density));
            sampleButton.setMinimumHeight(Math.round(40 * density));
            LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(
                    Math.round(48 * density),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sampleParams.setMarginStart(Math.round(8 * density));
            sampleButton.setLayoutParams(sampleParams);
            ButtonColors.apply(sampleButton);
            sampleButton.setOnClickListener(view -> play(sound, null));

            soundButtons.add(button);
            sampleButtons.add(sampleButton);
            row.addView(button);
            row.addView(sampleButton);
            soundOptions.addView(row);
        }

        configureNavigation();
    }

    private void configureNavigation() {
        View selector = activity.findViewById(R.id.alarm_sound_setting);
        for (int index = 0; index < soundButtons.size(); index++) {
            View button = soundButtons.get(index);
            View sampleButton = sampleButtons.get(index);
            button.setNextFocusLeftId(button.getId());
            button.setNextFocusRightId(sampleButton.getId());
            button.setNextFocusUpId(index == 0
                    ? selector.getId() : soundButtons.get(index - 1).getId());
            button.setNextFocusDownId(index == soundButtons.size() - 1
                    ? nextSectionId : soundButtons.get(index + 1).getId());
            sampleButton.setNextFocusLeftId(button.getId());
            sampleButton.setNextFocusRightId(sampleButton.getId());
            sampleButton.setNextFocusUpId(index == 0
                    ? selector.getId() : sampleButtons.get(index - 1).getId());
            sampleButton.setNextFocusDownId(index == soundButtons.size() - 1
                    ? nextSectionId : sampleButtons.get(index + 1).getId());
        }
    }

    private void select(AlarmSound sound) {
        current = sound;
        activity.getSharedPreferences(SETTINGS_PREFERENCES, Activity.MODE_PRIVATE)
                .edit().putString(ALARM_SOUND_KEY, sound.resourceName).apply();
        setOptionsExpanded(true, true);
    }

    private void setOptionsExpanded(boolean expanded, boolean focusSelectedSound) {
        if (soundOptions == null) {
            return;
        }
        soundOptions.setVisibility(expanded ? View.VISIBLE : View.GONE);
        Button selector = activity.findViewById(R.id.alarm_sound_setting);
        selector.setText(activity.getString(expanded
                        ? R.string.choose_alarm_sound_expanded
                        : R.string.choose_alarm_sound_collapsed,
                current == null ? activity.getString(R.string.no_alarm_sounds) : displayName(current.resourceName)));
        selector.setNextFocusDownId(expanded && !soundButtons.isEmpty()
                ? soundButtons.get(0).getId() : nextSectionId);
        if (expanded && focusSelectedSound && current != null) {
            for (int index = 0; index < sounds.size(); index++) {
                if (sounds.get(index) == current) {
                    soundButtons.get(index).requestFocus();
                    break;
                }
            }
        }
    }

    private String displayName(String resourceName) {
        String[] words = resourceName.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private void releaseActivePlayer() {
        if (activePlayer != null) {
            activePlayer.release();
            activePlayer = null;
        }
    }

    private void releasePlayer(MediaPlayer player) {
        player.release();
        if (activePlayer == player) {
            activePlayer = null;
        }
    }

    private void runAfterPlayback(Runnable afterPlayback) {
        if (afterPlayback != null) {
            afterPlayback.run();
        }
    }

    private static final class AlarmSound {
        final int resourceId;
        final String resourceName;

        AlarmSound(int resourceId, String resourceName) {
            this.resourceId = resourceId;
            this.resourceName = resourceName;
        }
    }
}
