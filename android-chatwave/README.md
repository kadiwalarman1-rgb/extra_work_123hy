# ChatWave — Android messenger prototype

ChatWave is an original Kotlin/Android messaging UI prototype with premium-style conveniences enabled locally: dark mode, accent themes, searchable chats, chat pinning, reactions, pinned messages, emoji insertion, and local attachment selection.

## Important scope

- This is **not Telegram** and does not use Telegram branding or copied Telegram assets.
- It is a local-device demo. Messages and chat preferences are stored on the device.
- It does not currently have accounts, cloud sync, internet message delivery, group servers, voice/video calling, or real remote file uploads. Those require a backend and additional implementation.
- No subscription gate is implemented for the features included in this prototype.

## Build on Windows

Requirements: Windows 10/11 x64, JDK 17+ (JDK 21 is fine), Android Studio, Android SDK Platform 35, and Android SDK Build-Tools 35.0.0.

Open PowerShell inside this folder and run:

```powershell
.\build.ps1 assembleDebug
```

The helper downloads the official Gradle 8.9 binary distribution and validates its SHA-256 hash before running the build. Android SDK must already be installed.

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Try it

Open the APK on an Android 8.0+ device. Tap a chat to open the conversation; send a local message, long-press a chat to pin it, long-press a message to react/pin/delete it, open the + button to make a local conversation, and visit Settings to change dark mode or accent color.
