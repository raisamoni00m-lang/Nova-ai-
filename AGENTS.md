# Nova — AI Voice Assistant (Android)

## Project Overview
Native Android app (Kotlin + Jetpack Compose). AI voice assistant with Bengali/English/Banglish support, phone control via Accessibility Service, smart memory, and privacy protection.

## Architecture
- **UI**: Jetpack Compose, Material 3, custom glassmorphism theme
- **AI**: Gemini API (via Firebase AI or direct REST) for natural language reasoning
- **Voice**: Android TTS + OmniVoice (k2-fsa/sherpa-onnx) for on-device neural TTS; Android SpeechRecognizer for ASR
- **Screen Reader**: Custom element-by-element screen reading via Accessibility Service
- **Data**: Room database (memories, chat history, automations, privacy audit)
- **Background**: Foreground service for wake word detection; Notification listener service

## Setup
- **Build tool**: Android Studio / Gradle (AGP 9.1.1, Kotlin 2.2.10)
- **Min SDK**: 24, Target SDK: 36
- **Key dependency**: sherpa-onnx AAR auto-downloaded via `downloadSherpaOnnx` gradle task (from GitHub releases v1.13.7)
- **Secrets**: `GEMINI_API_KEY` required for AI features (injected via secrets gradle plugin from .env)
- **Debug keystore**: `debug.keystore` at repo root (password: android)

## OmniVoice Integration (k2-fsa/sherpa-onnx)
- `OmniVoiceEngine.kt` wraps sherpa-onnx via reflection (avoids hard compile-time dependency)
- `OmniVoiceModelManager.kt` downloads VITS TTS models from HuggingFace to app private storage
- Models stored at `filesDir/omnivoice-models/<model_name>/`
- TTS falls back to Android TextToSpeech if OmniVoice models aren't loaded
- AAR download: `app/build.gradle.kts` has a `downloadSherpaOnnx` task that fetches `sherpa-onnx-1.13.7.aar` before `preBuild`

## Screen Reader
- `ScreenReaderController.kt` provides structured, element-by-element screen reading
- Uses `NovaAccessibilityService.rootInActiveWindow` to traverse the accessibility tree
- Supports next/previous navigation and auto-advance mode
- Voice commands: "screen reader start", "next element", "previous element", "stop reading"

## Verification
- This is a native Android app — cannot be previewed in a web browser
- Unit tests use Robolectric/Roborazzi (run with `./gradlew test`)
- Build APK with `./gradlew assembleDebug`
