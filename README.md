# Reverse Engineering Lab

A place for reverse-engineering practice and original implementations inspired by observed application behavior.

## Download APKs to your phone

- **[Download Bounce Lab (our original demo app APK)](https://github.com/kadiwalarman1-rgb/extra_work_123hy/releases/download/bounce-lab-v1.0.0/BounceLab-v1.0.0.apk)** — tap or drag to create colorful bouncing bubbles. The build workflow creates this release asset.
- **[Download the original ApiDemos sample APK](https://github.com/kadiwalarman1-rgb/extra_work_123hy/releases/download/apidemos-sample-v6.0.18/ApiDemos-debug.apk)** — the Appium sample app used during REA analysis.

Or open the [Releases page](https://github.com/kadiwalarman1-rgb/extra_work_123hy/releases) and select the APK under the relevant release's **Assets**.

## Android project: Bounce Lab

The `android-bounce-lab/` folder contains a small native Kotlin app inspired by the high-level interaction pattern observed in Android's public ApiDemos `BouncingBalls` sample. Tap or drag on the screen to create colorful bubbles that drop, squash, rebound, and fade away.

This is a clean implementation of the general interaction idea. It does not include the original APK, decompiled source files, original app assets, or branding.

### Build on Windows

1. Install [Android Studio](https://developer.android.com/studio).
2. In Android Studio's **SDK Manager**, install **Android SDK Platform 35** and Android SDK Build-Tools **35.0.0**.
3. Make sure Java 17 or newer is available. JDK 21 is suitable.
4. Open PowerShell in the `android-bounce-lab` folder.
5. Run:

   ```powershell
   .\build.ps1 assembleDebug
   ```

   On its first run, the script downloads the official Gradle 8.9 binary distribution and verifies its SHA-256 checksum before running the build. The Android SDK must already be installed.

6. The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

You can open the folder in VS Code to edit the Kotlin source. Use Android Studio's Device Manager to create an emulator, or connect an Android phone with USB debugging enabled, then install the built APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

- `app/src/main/java/com/example/bouncelab/MainActivity.kt` — custom Canvas view and bubble animation.
- `app/src/main/AndroidManifest.xml` — launcher activity.
- `build.ps1` — checksum-verified Gradle bootstrap/build helper for Windows.
