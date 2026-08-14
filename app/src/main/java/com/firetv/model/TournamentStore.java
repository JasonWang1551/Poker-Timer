package com.firetv.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TournamentStore {
    private static final String PREFERENCES_NAME = "poker_tournaments";
    private static final String CURRENT_TOURNAMENT_KEY = "current_tournament";
    private static final String SAVED_TOURNAMENTS_KEY = "saved_tournaments";

    private final SharedPreferences preferences;

    public TournamentStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void saveCurrent(Tournament tournament) {
        preferences.edit()
                .putString(CURRENT_TOURNAMENT_KEY, toJson(tournament).toString())
                .apply();
    }

    public Tournament loadCurrent() {
        String json = preferences.getString(CURRENT_TOURNAMENT_KEY, null);

        if (json == null) {
            return new Tournament();
        }

        return fromJson(json);
    }

    public void saveNamed(Tournament tournament) {
        JSONObject saved = readSavedObject();

        try {
            saved.put(tournament.getName(), toJson(tournament));
            preferences.edit()
                    .putString(SAVED_TOURNAMENTS_KEY, saved.toString())
                    .apply();
        } catch (JSONException ignored) {
            // JSONObject only rejects unsupported values; all stored values are JSON-safe.
        }
    }

    public Tournament loadNamed(String name) {
        JSONObject saved = readSavedObject();

        try {
            JSONObject tournamentJson = saved.getJSONObject(name);
            return fromJson(tournamentJson.toString());
        } catch (JSONException ignored) {
            return null;
        }
    }

    public List<String> getSavedNames() {
        JSONObject saved = readSavedObject();
        Iterator<String> keys = saved.keys();
        List<String> names = new ArrayList<>();

        while (keys.hasNext()) {
            names.add(keys.next());
        }

        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private JSONObject readSavedObject() {
        String json = preferences.getString(SAVED_TOURNAMENTS_KEY, "{}");

        try {
            return new JSONObject(json);
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    private JSONObject toJson(Tournament tournament) {
        JSONObject root = new JSONObject();
        JSONArray levels = new JSONArray();

        try {
            root.put("name", tournament.getName());
            root.put("currentLevelIndex", tournament.getCurrentLevelIndex());

            for (TournamentLevel level : tournament.getLevels()) {
                JSONObject levelJson = new JSONObject();
                levelJson.put("title", level.getTitle());
                levelJson.put("smallBlind", level.getSmallBlind());
                levelJson.put("bigBlind", level.getBigBlind());
                levelJson.put("minutes", level.getMinutes());
                levelJson.put("break", level.isBreak());
                levelJson.put("doubleBigBlind", level.isBigBlindDoubleSmallBlind());
                levelJson.put("indefiniteBreak", level.isIndefiniteBreak());
                levels.put(levelJson);
            }

            root.put("levels", levels);
        } catch (JSONException ignored) {
            // JSONObject only rejects unsupported values; all stored values are JSON-safe.
        }

        return root;
    }

    private Tournament fromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray levelArray = root.getJSONArray("levels");
            List<TournamentLevel> levels = new ArrayList<>();

            for (int index = 0; index < levelArray.length(); index++) {
                JSONObject levelJson = levelArray.getJSONObject(index);
                levels.add(new TournamentLevel(
                        levelJson.optString("title", ""),
                        levelJson.optInt("smallBlind", 25),
                        levelJson.optInt("bigBlind", 50),
                        levelJson.optInt("minutes", 10),
                        levelJson.optBoolean("break", false),
                        levelJson.optBoolean("doubleBigBlind", false),
                        levelJson.optBoolean("indefiniteBreak", false)));
            }

            if (levels.isEmpty() || levels.get(0).isBreak()) {
                return new Tournament();
            }

            Tournament tournament = new Tournament();
            tournament.setName(root.optString("name", ""));
            tournament.replaceLevels(levels);
            tournament.setCurrentLevelIndex(root.optInt("currentLevelIndex", 0));
            return tournament;
        } catch (JSONException ignored) {
            return new Tournament();
        }
    }
}
