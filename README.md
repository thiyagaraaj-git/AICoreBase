# AICoreBase

Android demo app for on-device AI using **Gemini Nano** (via AICore) and ML Kit GenAI APIs.

## What it does

- Checks if Gemini Nano is available and downloads the model if needed
- **Text Based** — type a prompt, get an on-device Gemini Nano reply
- **Voice Based** — speak, transcribe, get a Gemini Nano reply (shown on screen and read aloud via TTS)

## Requirements

- Android 15+ (minSdk 35)
- Physical device with AICore / Gemini Nano support
- Microphone permission for the voice demo

## Run

Open in Android Studio and run on a supported device.

```bash
./gradlew installDebug
```

## Stack

Kotlin · Jetpack Compose · ML Kit GenAI (`genai-prompt`, `genai-speech-recognition`)
