# English Tutor

An Android app for practising spoken English with an AI tutor. You talk, it answers, and
it slips grammar fixes in as suggestions rather than corrections.

Everything is voice: there is no text input anywhere in the conversation. Tap the
microphone, speak, and the reply comes back written and read aloud.

## What it does

- **Suggests topics to talk about**, generated for you from the profile built during
  onboarding — your level, your goal, what you are interested in. Tap one and the tutor
  opens the conversation.
- **Replies in conversation**, keeping the thread so answers follow on from what you said.
- **Offers a better phrasing** when something is off, shown as a "try saying" note beside
  the reply rather than as an error.
- **Reads every reply aloud** through Azure Neural TTS.
- **Keeps your sessions**, so you can reopen an old conversation and carry on.

You bring your own keys — the model's and Azure Speech's. Nothing is hosted, and no key
is compiled in: the build reads no secrets at all, so both are entered in the app.

## Running it

**You need:** Android Studio, a device or emulator on API 24+, an API key for either
Gemini or an OpenAI-compatible endpoint, and — for the tutor's voice — an Azure Speech
key.

1. Clone and open the project in Android Studio.
2. **Supply a debug keystore.** `debug.keystore` is deliberately untracked, so a fresh
   clone has none and `assembleDebug` will fail. Either drop your own in at the project
   root, or delete this line from `app/build.gradle.kts`:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
3. Run the app. Onboarding asks a few questions, then Settings → API configuration is
   where you add the model key, and Settings → Text to speech where you add the Azure
   Speech key, its region and (optionally) a voice. **Both keys are entered in the app.
   There is no build-time path for either.**

The app asks for microphone permission on the first tap of the mic, and needs network
access for the model and for speech synthesis.

## Building and testing

```bash
./gradlew verifyRoborazziDebug   # unit tests + screenshot comparison — the whole check
./gradlew assembleDebug          # debug APK
```

`verifyRoborazziDebug` is what CI runs, and it covers both: the unit tests around the
data path and 16 reference screenshots of the UI. Any JDK from 17 to 25 works.

**Do not run `recordRoborazziDebug` to make a red build green** — it overwrites the
reference images, which turns a regression into the new baseline. `CLAUDE.md` explains
when recording is the right move.

## How it is put together

Kotlin and Jetpack Compose, MVVM with unidirectional data flow, manual dependency
injection, Room for local storage. The model API is spoken to directly over HTTP, with
Gemini and OpenAI-compatible request shapes behind one interface, so you can point the app
at any provider that speaks either.

```
app/src/main/java/com/example/
  ui/          Compose screens, the design system, and their view models
  viewmodel/   UI state and intents
  data/
    local/     Room database, DAOs, entities
    remote/    Model APIs and the profile-driven service factory
    repository/  Coordinates local and remote
  manager/     TtsManager
```

Four further documents, each owning one thing:

| | |
|---|---|
| `ARCHITECTURE.md` | Tech stack, package boundaries, data-flow rules |
| `DESIGN.md` | The Organic design language, its tokens and components, and the deliberate departures |
| `CLAUDE.md` | How not to break the UI — read before changing it |
| `TASKS.md` | Working notes (untracked) |

## Licence

Not currently licensed for redistribution.
