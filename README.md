# Translator Pro

**AI Translator** keyboard and translation app for Android — built on the [OpenBoard](https://github.com/openboard-team/openboard) / AOSP LatinIME keyboard, with text, camera (OCR), voice, and conversation translation.

| | |
|---|---|
| **App name** | Translator Pro |
| **IME name** | AI Translator |
| **Application ID** | `com.translator.keyboard.translate.all.langugaes` |
| **Version** | 2.0 (versionCode 11) |
| **Min / Target SDK** | 27 / 36 |

## Features

- **Translator keyboard** — Soft keyboard with live translation support, suggestions, dictionaries, emoji, and clipboard history (including translate-from-clipboard)
- **Text translation** — Translate between languages with history and favorites
- **Camera / OCR** — Capture or pick an image, crop, and recognize text (Latin, Chinese, Japanese, Korean) via ML Kit, then translate
- **Voice & conversation** — Speech-to-text, text-to-speech, and two-way conversation mode
- **Phrasebook** — Ready-made phrases for common situations
- **Offline-capable translate** — Google ML Kit on-device translation models, with online fallback
- **Premium** — Play Billing subscriptions (weekly / monthly / yearly) for an ad-free experience
- **Ads** — AdMob (banner, interstitial, rewarded, native, app open) with mediation; placement config via Firebase Remote Config

## Modules

| Module | Description |
|--------|-------------|
| `:app` | Main app — IME + Translator Pro UI |
| `:country_data` | Country flags / metadata for language UI |
| `:keyboard-listener` | Soft-keyboard visibility helper |
| `:tools:make-keyboard-text` | Generates keyboard “more keys” / locale text tables |
| `:tools:make-emoji-keys` | Emoji key generation tooling |

## Tech stack

- **Language:** Kotlin + Java (translator UI mostly Kotlin; IME core mostly Java)
- **Build:** Android Gradle Plugin 8.13, Kotlin 2.1, Java 17, NDK 26.3 (16 KB page size), compile/target SDK 36
- **Translation / OCR:** ML Kit Translate, Language ID, Text Recognition (+ CJK)
- **Camera:** CameraX
- **Backend services:** Firebase Analytics, Crashlytics, Messaging, Remote Config, Storage
- **Monetization:** AdMob + mediation, Play Billing Library 8.3, UMP consent

## Getting started

### Requirements

- [Android Studio](https://developer.android.com/studio) (recent stable)
- JDK 17
- Android SDK 36
- NDK `26.3.11579264` (configured in the app module for JNI dictionaries)

### Clone & open

```sh
git clone https://github.com/ahmedaffan932/openboard.git
cd openboard
```

Open the project in Android Studio and sync Gradle.

### Build

```sh
# Debug APK
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

On Windows (PowerShell / CMD):

```bat
gradlew.bat :app:assembleDebug
```

### Keyboard text tooling

After editing locale “more keys” under `tools/make-keyboard-text/src/main/resources`:

```sh
./gradlew tools:make-keyboard-text:makeText
```

Emoji tooling: see [tools/make-emoji-keys/README.md](tools/make-emoji-keys/README.md).

## Project layout (high level)

```
app/src/main/java/
├── org/dslul/openboard/translator/pro/     # Translator Pro UI, ads, billing, FCM
└── org/dslul/openboard/inputmethod/       # Keyboard IME (LatinIME and related)
```

## License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

It is derived from OpenBoard / AOSP LatinIME. Upstream keyboard code and this fork’s modifications remain under GPL-3.0.

## Credits

- [OpenBoard](https://github.com/openboard-team/openboard) — FOSS keyboard base
- [AOSP LatinIME](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- [LineageOS LatinIME](https://review.lineageos.org/admin/repos/LineageOS/android_packages_inputmethods_LatinIME)
- [Simple Keyboard](https://github.com/rkkr/simple-keyboard)
- [Indic Keyboard](https://gitlab.com/indicproject/indic-keyboard)
