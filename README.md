# JARVIS — AI Messenger Assistant

Android assistant with Persian RTL UI, local preferences/history, Gemini 3.1 Flash-Lite, configurable rules, multiple API keys with rotation, and notification-based auto reply.

## Important
Auto-reply to third-party messengers depends on whether their Android notification exposes a writable RemoteInput action. The app does not bypass encryption or private APIs.

Gemini model: `gemini-3.1-flash-lite`.

API keys are entered by the user at runtime; never commit real keys to Git.

## Build
Open in Android Studio and run:
```
./gradlew assembleDebug
```
