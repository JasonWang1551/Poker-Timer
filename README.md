# Poker Timer

Poker Timer is an Android TV and Amazon Fire TV app for running home poker tournaments on a television. It keeps the current blinds and countdown visible from across the room, automatically advances through the tournament structure, and lets you build and save reusable tournament presets.

## Features

- Large, landscape timer designed for Android TV and Amazon Fire TV
- Configurable blind levels, antes, and level lengths
- Timed or indefinite breaks
- Previous and next level previews
- Automatic level advancement with an audible alert
- Tournament editor for adding, removing, and reordering levels and breaks
- Named tournament presets stored locally on the device
- Six color themes with immediate switching
- Selectable alarm sounds with preview buttons
- Optional, editable speech announcements for tournament starts, levels, blinds, antes, and breaks
- Scrollable settings with collapsible sections
- Remote-first navigation with no touchscreen required

## Remote controls

| Action | Control |
| --- | --- |
| Pause or resume | Select the Play/Pause button, or press Play/Pause |
| Advance one level | Select the Next button, or press Fast Forward |
| Reset the current level | Select the Reset button, or press Rewind once |
| Go back one level | Select the Previous button, or press Rewind twice within one second |
| Open the tournament menu | Select the Menu button, or press Menu or Info |
| Return from a menu | Press Back |
| Navigate the interface | Directional pad and Select |

All main actions are available in the bottom control row, so Android TV remotes only need a directional pad, Select, and Back. Dedicated media and menu buttons are optional shortcuts; no Amazon-specific buttons or keyboard are required. On non-Amazon devices, optional keyboard shortcuts are also available: `Space` pauses or resumes, `F` advances, `R` rewinds, and `Tab` opens the menu.

## Requirements

- Android Studio with JDK 17 or newer (Java source compatibility is 11)
- Android SDK 35
- An Android TV or Amazon Fire TV device, or a compatible TV emulator, running Android 5.1 (API 22) or newer

## Build and run

1. Clone the repository:

   ```bash
   git clone https://github.com/JasonWang1551/Poker-Timer.git
   cd Poker-Timer
   ```

2. Open the project in Android Studio and allow Gradle to sync.

3. Select an Android TV or Fire TV device or emulator and run the `app` configuration.

You can also build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

On Windows, use:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Tournament setup

Open the tournament menu to name, save, load, reset, or edit a tournament. In the editor, each level can have custom blinds, an optional ante, a title, and its own duration. Levels can inherit the preceding level duration, while breaks can use a fixed duration or wait indefinitely until resumed.

The big blind can automatically stay at twice the small blind. An ante can match the big blind or use a custom amount. Use the editor's move controls to reorder entries and its remove control to delete them.

### Saving and loading

Enter a tournament name before choosing **Save**. Saving again under the same name replaces that preset. **Load** opens the saved tournament list, where you can load or delete a preset. The app asks before loading over unsaved tournament changes and before deleting a preset.

Loading a preset returns it to the first level with the timer paused. **Reset Level** restores the current level's full duration; **Reset Tournament** returns to the beginning and pauses the timer.

Presets, the current tournament structure and level selection, and settings are stored locally on the device. Elapsed countdown time is not saved, so reopening the app does not restore a partially elapsed countdown.

### Running a tournament

Use the bottom row to go back a level, reset the current level, pause or resume, advance a level, or open the tournament menu. Unavailable previous/next controls are disabled. The screen shows the current level, blinds and ante, with previews of adjacent entries.

Timed levels advance automatically when their countdown ends. Timed breaks count down; indefinite breaks wait until you resume to continue to the next entry.

## Settings

Open the tournament menu, then choose **Settings**. Alarm sound, text to speech, and theme each have a collapsible section. Their contents are indented, and the menu scrolls when several sections are open. Use **Back to Menu** or the remote's Back button to return. Choices and edited speech phrases are saved automatically.

### Alarm sound

Choose **Alarm**, **Chime**, or **Tannoy**; Tannoy is the default. Select a sound's name to use it, or the play icon on its right to sample it. A preview does not change the selected alarm.

### Text to speech

All speech options start **off**. Each option has a checkbox, an editable phrase, and a preview icon on the right. Previews work even when the option is unchecked.

| Announcement | Default phrase |
| --- | --- |
| Tournament start | Good luck everyone in today's tournament |
| New level | Blinds are up next hand |
| Blinds intro | Blinds will be |
| Ante intro | with an ante of |
| Break | We are now on a break |

Blinds and ante share one checkbox, with separate editable introductory phrases. The app supplies the blind amounts and includes the ante only when the current level has one. Their preview uses the current level's amounts.

At a normal level transition, enabled speech is combined into one announcement in this order: **new level → blinds → ante**. When an alarm plays for the transition, speech follows it. Tournament-start speech includes the enabled blinds and ante announcement. Breaks use the break phrase. Pausing with the Play/Pause control stops current speech.

Both alarms and speech use the device's normal media volume. Speech uses the device's Android text-to-speech engine and default language; a working speech engine must be available for announcements.

### Theme

Choose **Slate**, **White**, **Green**, **Blue**, **Violet**, or **Red**. Changes apply immediately without restarting the timer. Selecting a theme keeps the theme list open so you can compare choices.

## Project structure

```text
app/src/main/
├── java/com/firetv/
│   ├── controller/   # Remote input, editor, themes, alarms, and speech
│   ├── data/         # Local tournament persistence
│   ├── model/        # Timer, tournament, and levels
│   └── ui/           # Main Android activity and shared button styling
└── res/              # TV layouts, themes, strings, and app artwork
```

## Color themes

Settings offers Slate, White, Green, Blue, Violet, and Red. Switching themes updates controls immediately and saves the choice without recreating the timer screen.

Each theme has five base color roles: `timerBackground`, `menuBackground`, `buttonBackground`, `themeAccent`, and `themeText`. Palette values live in `res/values/colors.xml`; `res/values/themes.xml` maps them to roles declared in `res/values/attrs.xml`. Dividers use the theme text color. Selection fills are derived from the menu background by `ButtonColors.applySelection`, with a contrast check against the text color.

`ButtonColors` derives the interaction states for the main controls, menus, and dynamically created editor buttons:
- Focus and hover share a 95% accent / 5% button blend, shifted by 2.5 percentage points of HSL lightness (lighter for dark buttons, darker for light buttons). The accent-heavy mix keeps the existing palettes visually close.
- Pressed shifts focus lightness by 10 percentage points, darker for bright fills and lighter for dark fills.
- Disabled uses 30% opacity for the button fill and independently for text/icons, allowing the actual surface behind each control to show through.
- Normal labels use the theme text color. Focused/pressed labels retain it when contrast permits; otherwise they use the main background color, then black/white as a fallback for readable contrast.

State colors are generated when applying the theme or creating a button, not on every interaction. Disabled takes precedence over every other state. Theme changes update existing controls without restarting the timer. A future custom palette editor can supply the same five roles without exposing individual interaction colors.

Text cursors use the accent color through `drawable/text_cursor.xml`. On API 29+, theme refresh replaces each cursor drawable and restores its visibility. On API 22-28, switching themes rebuilds the two hidden editing panels, restoring their saved field state before reconnecting listeners. The Settings panel and timer remain in place. Editor rows are rebuilt normally when the editor is next opened.

## Maintaining themes

You do not need to edit `MainActivity` to maintain themes.

| Change | File |
| --- | --- |
| Change an existing palette's color values | `app/src/main/res/values/colors.xml` |
| Assign colors to background/menu/button/accent/text roles | `app/src/main/res/values/themes.xml` |
| Adjust focus, pressed, or disabled effects for all themes | Named constants at the top of `app/src/main/java/com/firetv/ui/ButtonColors.java` |
| Register another theme | `ColorScheme` list at the top of `app/src/main/java/com/firetv/controller/ThemeController.java` |

To add a theme, define its colors and theme style, add its label in `strings.xml`, then add one `ColorScheme` entry. The buttons, click handlers, saved selection, and grid navigation are generated automatically. Keep existing enum names unchanged because they identify saved selections.

`ThemeController` handles loading/saving choices, updating the screen, and Android cursor compatibility. `MainActivity` only prepares the saved theme before inflation, refreshes the initial views, connects Settings navigation, and focuses the selected choice. The `rebindEditingPanels` callback reconnects application listeners after the older-Android cursor fallback replaces those views; it contains no color logic. All timer and tournament behavior stays in the activity and existing controllers.

Selection fill starts with a lighter shade of dark menus or darker shade of light menus. The calculation reduces that shift if needed to retain 4.5:1 text contrast, and corrects toward a safer shade if the base menu already has insufficient contrast. Translucent menu colors are composited over the main background before calculating the fill. Selected rows keep their accent outline; focus/hover/press adds the derived fill. No separate overlay or divider palette entries are needed.

## Maintaining alarms and speech

Add Android-compatible audio files to `app/src/main/res/raw` using lowercase underscore-separated names, such as `soft_bell.ogg`, then rebuild the app. `AlarmSoundController` builds an alphabetical list from those resources and turns the filenames into display labels; no new layout rows or registration entries are needed. Keep this folder for alarm audio because every raw resource is included. Existing filenames identify saved sound choices, so renaming one changes that identifier.

The speech form is declared in `app/src/main/res/layout/view_settings.xml`. Default phrases and labels live in `app/src/main/res/values/strings.xml`. `TextToSpeechController` connects the controls, saves phrases and checkboxes, builds announcements, and plays previews. Changing a default phrase affects settings that have not already been customized; saved text takes precedence.

TournamentStore in the data package owns local tournament persistence. MainActivity connects settings once; theme refresh reconnects only the editing panels replaced for older Android cursor support.
