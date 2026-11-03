# Pokédex Android

An Android Pokédex that identifies Pokémon from the phone camera. Image classification runs on-device with a bundled ONNX model; Pokémon details are retrieved from [PokéAPI](https://pokeapi.co/), so an internet connection is needed for species information and artwork.

## Features

- Classifies camera images across 1,025 Pokémon using the bundled MobileNetV3 ONNX model.
- Shows the predicted Pokémon and PokéAPI information, including types, height, weight, and evolution line artwork.
- Provides a top-five prediction list for checking possible false positives.
- Reads the displayed Pokémon’s name, localized genus when available, and description aloud.
- Supports English, Spanish, French, German, Italian, and Japanese. English is the default; the selected language is remembered. Missing localized descriptions fall back to English.
- Lets you tap the live camera preview to focus.

## Using the app

1. Install and open the app, then grant camera permission.
2. Press the green button to start the camera preview. Press it again to capture and classify an image.
3. Wait for the blue LED to blink while the image is processed. The predicted Pokémon and its details appear on the display.
4. Use the controls:
   - **Up / Down:** move through the information pages.
   - **Left / Right:** browse Pokémon by Pokédex number.
   - **Black round button:** replay the voice entry for the Pokémon currently displayed.
   - **Menu grille below the display:** choose the app language.
   - **Blue button:** show the top-five predictions.
   - **Help button (top right):** open the in-app manual.
5. Tap the display to hear the current Pokémon’s voice entry again.

## Build an APK

### Requirements

- Android Studio with Android SDK Platform 37 installed.
- JDK 17 or newer (Android Studio’s bundled JDK is recommended).
- An Android device or emulator running Android 12 (API 31) or newer to install the app.

The project uses the Gradle wrapper, so a separate Gradle installation is not required. Open the project in Android Studio and allow Gradle sync to finish, or run these commands from the project root in PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

The installable, debug-signed APK is created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

You can install it on a connected device with:

```powershell
.\gradlew.bat :app:installDebug
```

To build the optimized release variant, run:

```powershell
.\gradlew.bat :app:assembleRelease
```

The release APK is at `app\build\outputs\apk\release\app-release-unsigned.apk`. It is unsigned by default; configure a release signing key in your local Gradle setup before distributing it. Do not commit signing keys or passwords.

## Model files

The classifier and its class-to-Pokédex mapping are bundled in `app\src\main\assets\`:

- `pokemon_classifier_1025.onnx`
- `pokemon_labels_1025.json`

To use a different model, replace the ONNX file and update the labels JSON to match its output classes and Pokédex IDs. Keep the filenames expected by the app, or update the asset-loading code as well.

To train your own model, you can check [this Github repo](https://github.com/Sutadasuto/pokedex_project).

## Notes

- Classification is performed locally; PokéAPI lookups and remote Pokémon artwork require internet access.
- The app requests camera and internet permissions.
- APKs and generated build output are excluded from Git. Build artifacts can be recreated with the commands above.
