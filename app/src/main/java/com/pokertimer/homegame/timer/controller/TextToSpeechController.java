package com.pokertimer.homegame.timer.controller;

import com.pokertimer.homegame.timer.ui.ButtonColors;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pokertimer.homegame.timer.R;
import com.pokertimer.homegame.timer.model.TournamentLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Owns the independently enabled speech announcements in Settings. */
public final class TextToSpeechController {
    public interface CurrentLevelProvider {
        TournamentLevel get();
    }

    private static final String SETTINGS_PREFERENCES = "poker_timer_settings";
    private static final String SPEECH_ENABLED_PREFIX = "speech_enabled_";
    private static final String SPEECH_TEXT_PREFIX = "speech_text_";
    private static final String SPEECH_ANTE_TEXT_KEY = "speech_text_ante_intro";

    private enum Announcement {
        TOURNAMENT_START("tournament_start", R.string.speech_start_text),
        NEW_LEVEL("new_level", R.string.speech_new_level_text),
        BLINDS("blinds", R.string.speech_blinds_text),
        BREAK("break", R.string.speech_break_text);

        final String preferenceName;
        final int text;

        Announcement(String preferenceName, int text) {
            this.preferenceName = preferenceName;
            this.text = text;
        }
    }

    private final Activity activity;
    private final SharedPreferences preferences;
    private final CurrentLevelProvider currentLevel;
    private final List<View> announcementOptions = new ArrayList<>();
    private final List<View> previewButtons = new ArrayList<>();
    private final List<View> textFields = new ArrayList<>();
    private final List<String> alternateEngines = new ArrayList<>();
    private EditText anteTextField;
    private String pendingText;
    private String currentSpeechText;
    private String activeUtteranceId;
    private TextToSpeech textToSpeech;
    private boolean ready;
    private boolean speechUnavailable;
    private int engineIndex = -1;
    private int engineGeneration;
    private int utteranceNumber;
    private LinearLayout options;
    private TextView unavailableMessage;

    public TextToSpeechController(Activity activity, CurrentLevelProvider currentLevel) {
        this.activity = activity;
        this.currentLevel = currentLevel;
        preferences = activity.getSharedPreferences(SETTINGS_PREFERENCES, Activity.MODE_PRIVATE);
        startEngine(null);
    }

    private void startEngine(String name) {
        ready = false;
        int generation = ++engineGeneration;
        if (textToSpeech != null) textToSpeech.shutdown();
        TextToSpeech.OnInitListener listener = status ->
                activity.runOnUiThread(() -> initializeSpeech(status, generation));
        textToSpeech = name == null
                ? new TextToSpeech(activity, listener)
                : new TextToSpeech(activity, listener, name);
    }

    private void initializeSpeech(int status, int generation) {
        if (generation != engineGeneration || textToSpeech == null) return;
        if (alternateEngines.isEmpty()) {
            String defaultEngine = textToSpeech.getDefaultEngine();
            for (TextToSpeech.EngineInfo info : textToSpeech.getEngines()) {
                if (info.name != null && !info.name.equals(defaultEngine)) {
                    alternateEngines.add(info.name);
                }
            }
        }
        if (status != TextToSpeech.SUCCESS) {
            tryNextEngine();
            return;
        }
        textToSpeech.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        Voice defaultVoice = textToSpeech.getDefaultVoice();
        int languageStatus = textToSpeech.setLanguage(Locale.getDefault());
        if (languageStatus < TextToSpeech.LANG_AVAILABLE) {
            languageStatus = textToSpeech.setLanguage(Locale.US);
        }
        if (languageStatus < TextToSpeech.LANG_AVAILABLE) {
            languageStatus = textToSpeech.setLanguage(Locale.ENGLISH);
        }
        if (languageStatus < TextToSpeech.LANG_AVAILABLE && defaultVoice != null) {
            textToSpeech.setVoice(defaultVoice);
        }
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }
            @Override public void onDone(String utteranceId) { }
            @Override public void onError(String utteranceId) {
                retrySpeech(utteranceId, generation);
            }
            @Override public void onError(String utteranceId, int errorCode) {
                retrySpeech(utteranceId, generation);
            }
        });
        ready = true;
        setSpeechUnavailable(false);
        if (pendingText != null) {
            String queuedText = pendingText;
            pendingText = null;
            speakText(queuedText);
        }
    }

    private boolean tryNextEngine() {
        if (engineIndex + 1 >= alternateEngines.size()) {
            ready = false;
            pendingText = null;
            setSpeechUnavailable(true);
            return false;
        }
        startEngine(alternateEngines.get(++engineIndex));
        return true;
    }

    private void retrySpeech(String utteranceId, int generation) {
        activity.runOnUiThread(() -> {
            if (generation != engineGeneration || !utteranceId.equals(activeUtteranceId)) return;
            pendingText = currentSpeechText;
            tryNextEngine();
        });
    }

    private void setSpeechUnavailable(boolean unavailable) {
        speechUnavailable = unavailable;
        if (unavailableMessage != null) {
            unavailableMessage.setVisibility(unavailable ? View.VISIBLE : View.GONE);
        }
    }

    /** Connects the speech controls declared in the Settings layout. */
    public void bindSettings(int nextSectionId) {
        options = activity.findViewById(R.id.text_to_speech_options_container);
        unavailableMessage = activity.findViewById(R.id.tts_unavailable_message);
        setSpeechUnavailable(speechUnavailable);
        announcementOptions.clear();
        previewButtons.clear();
        textFields.clear();
        bindOption(Announcement.TOURNAMENT_START, R.id.tts_start_checkbox,
                R.id.tts_start_preview, R.id.tts_start_text);
        bindOption(Announcement.NEW_LEVEL, R.id.tts_new_level_checkbox,
                R.id.tts_new_level_preview, R.id.tts_new_level_text);
        bindOption(Announcement.BLINDS, R.id.tts_blinds_checkbox,
                R.id.tts_blinds_preview, R.id.tts_blinds_text);
        anteTextField = activity.findViewById(R.id.tts_ante_text);
        bindTextField(anteTextField, SPEECH_ANTE_TEXT_KEY, getAnteText());
        bindOption(Announcement.BREAK, R.id.tts_break_checkbox,
                R.id.tts_break_preview, R.id.tts_break_text);

        configureNavigation(nextSectionId);
        Button selector = activity.findViewById(R.id.text_to_speech_selector);
        selector.setOnClickListener(view -> setOptionsExpanded(
                options.getVisibility() != View.VISIBLE, nextSectionId));
        selector.setNextFocusLeftId(selector.getId());
        selector.setNextFocusRightId(selector.getId());
        setOptionsExpanded(false, nextSectionId);
    }

    private void bindOption(Announcement announcement, int checkboxId, int previewId, int textId) {
        CheckBox option = activity.findViewById(checkboxId);
        ImageButton preview = activity.findViewById(previewId);
        EditText textField = activity.findViewById(textId);
        option.setChecked(isEnabled(announcement));
        applyColors(option);
        option.setOnCheckedChangeListener((button, checked) -> preferences.edit()
                .putBoolean(SPEECH_ENABLED_PREFIX + announcement.preferenceName, checked).apply());
        preview.setOnClickListener(view -> preview(announcement));
        bindTextField(textField, textKey(announcement), getText(announcement));
        announcementOptions.add(option);
        previewButtons.add(preview);
        textFields.add(textField);
    }

    private void bindTextField(EditText field, String key, String value) {
        field.setText(value);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                preferences.edit().putString(key, editable.toString()).apply();
            }
        });
    }

    public void announceTournamentStart(TournamentLevel level) {
        StringBuilder announcement = new StringBuilder();
        appendEnabledText(announcement, Announcement.TOURNAMENT_START,
                getText(Announcement.TOURNAMENT_START));
        appendBlindsAndAnte(announcement, level);
        speakText(announcement.toString());
    }

    /** Announces a playable level in the order: new level, blinds, then ante. */
    public void announceLevel(TournamentLevel level) {
        StringBuilder announcement = new StringBuilder();
        appendEnabledText(announcement, Announcement.NEW_LEVEL, getText(Announcement.NEW_LEVEL));
        appendBlindsAndAnte(announcement, level);
        speakText(announcement.toString());
    }

    /** Repeats only the current level's blinds and ante after a running level is reset. */
    public void announceBlindsAndAnte(TournamentLevel level) {
        StringBuilder announcement = new StringBuilder();
        appendBlindsAndAnte(announcement, level);
        speakText(announcement.toString());
    }

    private void appendBlindsAndAnte(StringBuilder announcement, TournamentLevel level) {
        if (isEnabled(Announcement.BLINDS)) {
            appendText(announcement, getText(Announcement.BLINDS)
                    + " " + level.getSmallBlind() + ", " + level.getBigBlind());
        }
        if (level.hasAnte() && isEnabled(Announcement.BLINDS)) {
            appendText(announcement, getAnteText() + " " + level.getAnte());
        }
    }

    public void announceBreak() {
        if (isEnabled(Announcement.BREAK)) {
            speakText(getText(Announcement.BREAK));
        }
    }

    public void shutdown() {
        stop();
        ready = false;
        engineGeneration++;
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    public void stop() {
        pendingText = null;
        currentSpeechText = null;
        activeUtteranceId = null;
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void preview(Announcement announcement) {
        if (announcement == Announcement.BLINDS) {
            TournamentLevel level = currentLevel.get();
            StringBuilder previewText = new StringBuilder();
            appendText(previewText, getText(Announcement.BLINDS) + " "
                    + level.getSmallBlind() + ", " + level.getBigBlind());
            if (level.hasAnte()) {
                appendText(previewText, getAnteText() + " " + level.getAnte());
            }
            speakText(previewText.toString());
            return;
        }
        speakText(getText(announcement));
    }

    private String getText(Announcement announcement) {
        return preferences.getString(textKey(announcement), activity.getString(announcement.text));
    }

    private String textKey(Announcement announcement) {
        return SPEECH_TEXT_PREFIX + announcement.preferenceName;
    }

    private String getAnteText() {
        return preferences.getString(SPEECH_ANTE_TEXT_KEY,
                activity.getString(R.string.speech_ante_text));
    }

    private void appendEnabledText(StringBuilder announcement, Announcement type, String text) {
        if (isEnabled(type)) {
            appendText(announcement, text);
        }
    }

    private void appendText(StringBuilder announcement, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (announcement.length() > 0) {
            announcement.append(". ");
        }
        announcement.append(text.trim());
    }

    private boolean isEnabled(Announcement announcement) {
        return preferences.getBoolean(SPEECH_ENABLED_PREFIX + announcement.preferenceName, false);
    }

    private void speakText(String text) {
        if (text == null || text.trim().isEmpty() || textToSpeech == null) {
            return;
        }
        if (!ready) {
            pendingText = text;
            return;
        }
        currentSpeechText = text;
        activeUtteranceId = "poker_timer_" + (++utteranceNumber);
        Bundle parameters = new Bundle();
        parameters.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
        int queueStatus = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, parameters,
                activeUtteranceId);
        if (queueStatus != TextToSpeech.SUCCESS) {
            pendingText = text;
            tryNextEngine();
        }
    }

    private void configureNavigation(int nextSectionId) {
        View selector = activity.findViewById(R.id.text_to_speech_selector);
        for (int index = 0; index < announcementOptions.size(); index++) {
            View option = announcementOptions.get(index);
            View preview = previewButtons.get(index);
            View textField = textFields.get(index);
            option.setNextFocusLeftId(option.getId());
            option.setNextFocusRightId(preview.getId());
            option.setNextFocusUpId(index == 0
                    ? selector.getId() : textFields.get(index - 1).getId());
            option.setNextFocusDownId(textField.getId());
            preview.setNextFocusLeftId(option.getId());
            preview.setNextFocusRightId(preview.getId());
            preview.setNextFocusUpId(index == 0
                    ? selector.getId() : textFields.get(index - 1).getId());
            preview.setNextFocusDownId(textField.getId());
            textField.setNextFocusLeftId(textField.getId());
            textField.setNextFocusRightId(textField.getId());
            textField.setNextFocusUpId(option.getId());
            textField.setNextFocusDownId(index == textFields.size() - 1
                    ? nextSectionId : announcementOptions.get(index + 1).getId());
        }
        if (anteTextField != null) {
            anteTextField.setNextFocusLeftId(anteTextField.getId());
            anteTextField.setNextFocusRightId(anteTextField.getId());
            anteTextField.setNextFocusUpId(textFields.get(2).getId());
            anteTextField.setNextFocusDownId(announcementOptions.get(3).getId());
            textFields.get(2).setNextFocusDownId(anteTextField.getId());
            announcementOptions.get(3).setNextFocusUpId(anteTextField.getId());
            previewButtons.get(3).setNextFocusUpId(anteTextField.getId());
        }
    }

    private void setOptionsExpanded(boolean expanded, int nextSectionId) {
        options.setVisibility(expanded ? View.VISIBLE : View.GONE);
        Button selector = activity.findViewById(R.id.text_to_speech_selector);
        selector.setText(expanded ? R.string.text_to_speech_expanded : R.string.text_to_speech_collapsed);
        selector.setNextFocusDownId(expanded && !announcementOptions.isEmpty()
                ? announcementOptions.get(0).getId() : nextSectionId);
    }

    private void applyColors(CheckBox option) {
        int text = ButtonColors.themeColor(activity, R.attr.themeText);
        int accent = ButtonColors.themeColor(activity, R.attr.themeAccent);
        option.setTextColor(text);
        option.setButtonTintList(new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_checked}, new int[]{}
        }, new int[]{ButtonColors.disabledColor(text), accent, text}));
    }
}
