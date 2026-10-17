# Bounce Lab — Android

A small native Android/Kotlin demo: tap the screen to create a glossy colored bubble. Bubbles fall toward the bottom, squash on impact, bounce back, and fade away. Dragging creates a short trail of bubbles.

## Requirements

- Windows 10/11 x64
- Android Studio and Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- JDK 17 or newer (JDK 21 works)
- Internet access for the first Gradle/plugin dependency download

The project uses Android Gradle Plugin 8.7.3 and Kotlin 2.0.21. The build helper downloads Gradle 8.9 from the official Gradle distribution host and checks its SHA-256 checksum before execution. The Android SDK is not bundled in this repository.

## Build a debug APK

Open PowerShell in this folder and run:

```powershell
.\build.ps1 assembleDebug
```

The first run downloads and verifies Gradle. If the Android SDK is installed in a non-default location, set `ANDROID_HOME` to that location before building. The script also checks the common Windows SDK path under `%LOCALAPPDATA%\Android\Sdk`.

The resulting APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected, USB-debugging-enabled Android device:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or start an Android emulator from Android Studio's Device Manager.

## Implementation notes

- Uses Android's Canvas and ValueAnimator APIs.
- Bubbles are created from touch input, painted with a radial gradient, and animated through a fall, squash, rebound, and fade sequence.
- No third-party runtime libraries, network permissions, or copied APK assets are used.
- The behavior is an original implementation of a general animation pattern studied from a public sample; it is not a source-code reconstruction of the entire sample app.
