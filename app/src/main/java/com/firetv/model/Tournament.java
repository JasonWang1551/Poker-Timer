package com.firetv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tournament {
    private static final int DEFAULT_LEVEL_MINUTES = 20;
    private static final int[] DEFAULT_SMALL_BLINDS = {
            25,
            50,
            75,
            100,
            150,
            200,
            250,
            300,
            400,
            500,
            800,
            1_000,
            1_500,
            2_000,
            3_000
    };

    private final List<TournamentLevel> levels = new ArrayList<>();
    private int currentLevelIndex;
    private String name = "";

    public Tournament() {
        for (int index = 0; index < DEFAULT_SMALL_BLINDS.length; index++) {
            int smallBlind = DEFAULT_SMALL_BLINDS[index];
            levels.add(new TournamentLevel(
                    "",
                    smallBlind,
                    smallBlind * 2,
                    DEFAULT_LEVEL_MINUTES,
                    false,
                    true,
                    false,
                    index == 0,
                    false,
                    true,
                    smallBlind*2));
        }
    }

    public List<TournamentLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void replaceWith(Tournament other) {
        name = other.name;
        levels.clear();

        for (TournamentLevel level : other.levels) {
            levels.add(new TournamentLevel(
                    level.getTitle(),
                    level.getSmallBlind(),
                    level.getBigBlind(),
                    level.getMinutes(),
                    level.isBreak(),
                    level.isBigBlindDoubleSmallBlind(),
                    level.isIndefiniteBreak(),
                    level.hasOwnTime(),
                    level.hasAnte(),
                    level.isAnteEqualToBigBlind(),
                    level.getCustomAnte()));
        }

        currentLevelIndex = Math.min(other.currentLevelIndex, levels.size() - 1);
    }

    public void setCurrentLevelIndex(int index) {
        currentLevelIndex = Math.max(0, Math.min(index, levels.size() - 1));
    }

    public void replaceLevels(List<TournamentLevel> replacementLevels) {
        if (replacementLevels.isEmpty()) {
            return;
        }

        levels.clear();
        levels.addAll(replacementLevels);
        ensureFirstLevelHasOwnTime();
        currentLevelIndex = Math.min(currentLevelIndex, levels.size() - 1);
    }

    public TournamentLevel getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public TournamentLevel getLevel(int index) {
        return levels.get(index);
    }

    public int size() {
        return levels.size();
    }

    public boolean hasSameConfigurationAs(Tournament other) {
        if (other == null || !name.equals(other.name) || levels.size() != other.levels.size()) {
            return false;
        }

        for (int index = 0; index < levels.size(); index++) {
            TournamentLevel level = levels.get(index);
            TournamentLevel otherLevel = other.levels.get(index);

            if (!level.getTitle().equals(otherLevel.getTitle())
                    || level.getSmallBlind() != otherLevel.getSmallBlind()
                    || level.getBigBlind() != otherLevel.getBigBlind()
                    || level.getMinutes() != otherLevel.getMinutes()
                    || level.isBreak() != otherLevel.isBreak()
                    || level.isBigBlindDoubleSmallBlind()
                    != otherLevel.isBigBlindDoubleSmallBlind()
                    || level.isIndefiniteBreak() != otherLevel.isIndefiniteBreak()
                    || level.hasOwnTime() != otherLevel.hasOwnTime()
                    || level.hasAnte() != otherLevel.hasAnte()
                    || level.isAnteEqualToBigBlind()
                    != otherLevel.isAnteEqualToBigBlind()
                    || level.getCustomAnte() != otherLevel.getCustomAnte()) {
                return false;
            }
        }

        return true;
    }

    public boolean advance() {
        if (currentLevelIndex >= levels.size() - 1) {
            return false;
        }

        currentLevelIndex++;
        return true;
    }

    public boolean goBack() {
        if (currentLevelIndex <= 0) {
            return false;
        }

        currentLevelIndex--;
        return true;
    }

    public void reset() {
        currentLevelIndex = 0;
    }

    public int addLevel() {
        TournamentLevel previous = getLastPlayableLevel();
        levels.add(new TournamentLevel(
                "",
                previous.getSmallBlind(),
                previous.getBigBlind(),
                previous.getMinutes(),
                false,
                previous.isBigBlindDoubleSmallBlind(),
                false,
                false,
                previous.hasAnte(),
                previous.isAnteEqualToBigBlind(),
                previous.getCustomAnte()));
        return levels.size() - 1;
    }

    public int addBreak() {
        if (!canAddBreak()) {
            return levels.size() - 1;
        }

        levels.add(new TournamentLevel(
                "",
                0,
                0,
                10,
                true,
                false,
                false,
                true,
                false,
                false,
                0));
        return levels.size() - 1;
    }

    public boolean canAddBreak() {
        return !levels.get(levels.size() - 1).isBreak();
    }

    public int getEffectiveMinutes(int index) {
        TournamentLevel selectedLevel = levels.get(index);

        if (selectedLevel.hasOwnTime()) {
            return selectedLevel.getMinutes();
        }

        for (int position = index - 1; position >= 0; position--) {
            TournamentLevel previousLevel = levels.get(position);

            if (!previousLevel.isBreak() && previousLevel.hasOwnTime()) {
                return previousLevel.getMinutes();
            }
        }

        return selectedLevel.getMinutes();
    }

    public boolean isFirstPlayableLevel(int index) {
        return getPlayableLevelNumber(index) == 1
                && !levels.get(index).isBreak();
    }

    private void ensureFirstLevelHasOwnTime() {
        for (TournamentLevel level : levels) {
            if (!level.isBreak()) {
                level.setHasOwnTime(true);
                return;
            }
        }
    }

    private TournamentLevel getLastPlayableLevel() {
        for (int index = levels.size() - 1; index >= 0; index--) {
            TournamentLevel level = levels.get(index);

            if (!level.isBreak()) {
                return level;
            }
        }

        throw new IllegalStateException("Tournament must contain a playable level");
    }

    public int getPlayableLevelNumber(int index) {
        int levelNumber = 0;

        for (int position = 0; position <= index; position++) {
            if (!levels.get(position).isBreak()) {
                levelNumber++;
            }
        }

        return levelNumber;
    }

    public int remove(int index) {
        if (!canRemove(index)) {
            return index;
        }

        TournamentLevel currentLevel = getCurrentLevel();
        boolean removeFollowingBreak = index == 0
                && levels.size() > 1
                && levels.get(1).isBreak();
        levels.remove(index);

        if (removeFollowingBreak) {
            levels.remove(0);
        }

        ensureFirstLevelHasOwnTime();

        int retainedCurrentIndex = levels.indexOf(currentLevel);

        if (retainedCurrentIndex >= 0) {
            currentLevelIndex = retainedCurrentIndex;
        } else {
            currentLevelIndex = Math.min(index, levels.size() - 1);
        }

        return Math.min(index, levels.size() - 1);
    }

    public int move(int fromIndex, int toIndex) {
        int direction = Integer.compare(toIndex, fromIndex);

        if (!canMove(fromIndex, direction)) {
            return fromIndex;
        }

        int boundedTarget = Math.max(0, Math.min(toIndex, levels.size() - 1));

        if (fromIndex == boundedTarget) {
            return fromIndex;
        }

        TournamentLevel currentLevel = getCurrentLevel();
        TournamentLevel movedLevel = levels.remove(fromIndex);
        levels.add(boundedTarget, movedLevel);
        ensureFirstLevelHasOwnTime();
        currentLevelIndex = levels.indexOf(currentLevel);
        return boundedTarget;
    }

    public boolean canMove(int index, int direction) {
        if (direction == 0) {
            return false;
        }

        int targetIndex = index + direction;

        if (targetIndex < 0 || targetIndex >= levels.size()) {
            return false;
        }

        List<TournamentLevel> proposedOrder = new ArrayList<>(levels);
        TournamentLevel movedLevel = proposedOrder.remove(index);
        proposedOrder.add(targetIndex, movedLevel);

        if (proposedOrder.get(0).isBreak()) {
            return false;
        }

        return true;
    }

    public boolean canRemove(int index) {
        if (levels.get(index).isBreak()) {
            return true;
        }

        int playableLevels = 0;

        for (TournamentLevel level : levels) {
            if (!level.isBreak()) {
                playableLevels++;
            }
        }

        return playableLevels > 1;
    }

    public TournamentLevel getNextPlayableLevelAfter(int index) {
        for (int position = index + 1; position < levels.size(); position++) {
            TournamentLevel level = levels.get(position);

            if (!level.isBreak()) {
                return level;
            }
        }

        return null;
    }

    public TournamentLevel getPreviousPlayableLevelBefore(int index) {
        for (int position = index - 1; position >= 0; position--) {
            TournamentLevel level = levels.get(position);

            if (!level.isBreak()) {
                return level;
            }
        }

        return null;
    }
}
