# One Button Translate

A minimal Android app for fast, distraction-free translation. No cookie banners,
no loading pages — open it, type, tap, read.

## What it does (v0.1)

- First launch → Settings screen (forced until an API key is saved).
- Main screen → text field, **Translate** button, result. Gear icon → settings.
  Swap arrow → flips source/target language.
- Two providers, switchable in Settings:
  - **DeepL** (recommended) — free tier gives 500,000 chars/month, best quality
    for European languages. Free-plan keys end in `:fx`; the app auto-routes to
    the correct endpoint (`api-free.deepl.com` vs `api.deepl.com`).
  - **OpenAI** — pay-per-token (~$0.15/M for `gpt-4o-mini`). Model name is
    configurable so you can pin a newer/older one without rebuilding.
- API keys never leave the device. They live in app-private DataStore
  preferences and are excluded from cloud backup and device-to-device transfer.

## Building locally

Requirements: **JDK 21** installed somewhere Gradle can auto-discover it
(`/usr/lib/jvm`, `$JAVA_HOME`, `$PATH`, SDKMAN, asdf, Android Studio's bundled
JDK — all work) plus an Android SDK with **platforms;android-37.0** and
**build-tools;37.0.0**. The daemon JVM is pinned to 21 via
`gradle/gradle-daemon-jvm.properties`, so the system `JAVA_HOME` is irrelevant.

`local.properties` points at `/opt/android-sdk` by default; change `sdk.dir` if
your SDK lives elsewhere.

```bash
# Debug APK
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Getting an API key

- **DeepL Free**: https://www.deepl.com/pro-api → "DeepL API Free". No credit card.
  Copy the key (ends in `:fx`) into Settings → DeepL API key.
- **OpenAI**: https://platform.openai.com/api-keys → create key. Paste into
  Settings → OpenAI API key. Default model is `gpt-4o-mini`.

## Roadmap

- Speech input via Android's `SpeechRecognizer` (no extra API needed).
- Speech output via Android's `TextToSpeech` (no extra API needed).
- Optional third provider (Anthropic Claude) — add as another `Translator`
  implementation alongside DeepL/OpenAI.

## Project layout

```
app/src/main/kotlin/com/onebuttontranslate/
├── MainActivity.kt              # routing: settings vs translate
├── data/
│   ├── Provider.kt              # DEEPL | OPENAI
│   └── Settings.kt              # DataStore-backed settings repo
├── translate/
│   ├── Translator.kt            # interface + HttpClient seam
│   ├── DeepLTranslator.kt
│   ├── OpenAITranslator.kt
│   └── TranslatorFactory.kt
└── ui/
    ├── Theme.kt
    ├── TranslateViewModel.kt
    ├── TranslateScreen.kt
    └── SettingsScreen.kt
```

Tests live under `app/src/test/kotlin/com/onebuttontranslate/translate/` and
exercise body construction, response parsing, and HTTP error mapping with a
fake `HttpClient` — no network required.
