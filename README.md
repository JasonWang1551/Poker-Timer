# Poker Timer

Poker Timer is a Fire TV Stick app for running home poker tournaments on a television. It keeps the current blinds and countdown visible from across the room, automatically advances through the tournament structure, and lets you build and save reusable tournament presets.

## Features

- Large, landscape timer designed for Amazon Fire TV Stick
- Configurable blind levels, antes, and level lengths
- Timed or indefinite breaks
- Previous and next level previews
- Automatic level advancement with an audible alert
- Tournament editor for adding, removing, and reordering levels and breaks
- Named tournament presets stored locally on the device
- Remote-first navigation with no touchscreen required

## Remote controls

| Action | Control |
| --- | --- |
| Pause or resume | Select the center timer, or press Play/Pause |
| Advance one level | Select the next-level panel, or press Fast Forward |
| Reset the current level | Press Rewind once |
| Go back one level | Press Rewind twice within one second |
| Open or close the tournament menu | Press Menu or Info |
| Navigate the interface | Directional pad and Select |

The app is currently designed for the Fire TV Stick remote. On non-Amazon Android TV devices, the standard remote controls are not fully supported and a connected keyboard is required: `Space` pauses or resumes, `F` advances, `R` rewinds, and `Tab` opens the menu.

## Requirements

- Android Studio with JDK 11 or newer
- Android SDK 35
- An Amazon Fire TV Stick or compatible Fire TV emulator running Android 5.1 (API 22) or newer

## Build and run

1. Clone the repository:

   ```bash
   git clone https://github.com/JasonWang1551/Poker-Timer.git
   cd Poker-Timer
   ```

2. Open the project in Android Studio and allow Gradle to sync.

3. Select a Fire TV Stick or compatible Fire TV emulator and run the `app` configuration.

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

Tournament presets and the current tournament state are stored locally on the device.

## Project structure

```text
app/src/main/
├── java/com/firetv/
│   ├── controller/   # Remote input and screen controls
│   ├── model/        # Timer, tournament, levels, and persistence
│   └── ui/           # Main Android activity
└── res/              # TV layouts, themes, strings, and app artwork
```
