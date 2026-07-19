# Tasks

## Remove the build-time Gemini API key; make the user-entered key the only credential

### Context

The app currently bundles a Gemini API key at build time: the Secrets Gradle Plugin reads
`GEMINI_API_KEY` from `.env` (template in `.env.example`) and exposes it as
`BuildConfig.GEMINI_API_KEY`. Since the settings screen was added
(`app/src/main/java/com/example/ui/SettingsScreen.kt`), users can enter their own Gemini or
OpenAI key, and the built-in key only serves as a fallback when the Gemini key field is left
blank. Shipping a key inside the APK is a leak waiting to happen, so the built-in key must be
removed entirely: the key typed into Settings becomes the only way to call Gemini.

### Required changes

1. **`app/src/main/java/com/example/data/remote/GeminiApiService.kt`**
   - Remove the `import com.example.BuildConfig` and the default value
     `apiKey: String = BuildConfig.GEMINI_API_KEY` — make `apiKey` a required constructor
     parameter. (`app/src/test/java/com/example/ChatSessionTest.kt` already passes
     `apiKey = "test-key"` explicitly, so it keeps compiling.)

2. **`app/src/main/java/com/example/data/remote/ConfigurableAiService.kt`**
   - Remove the fallback `settings.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }` and the
     `BuildConfig` import; pass the stored key through as-is.
   - When the selected provider's API key is blank, throw a dedicated exception (e.g. add
     `class MissingApiKeyException : Exception(...)` next to `AiApiException` in
     `AiModelService.kt`) *before* attempting a network call, so callers can distinguish
     "not configured" from "request failed".

3. **User-facing handling of a missing key**
   - `app/src/main/java/com/example/ui/ChatViewModel.kt` (`onSpeechResult` catch block): catch the
     missing-key exception separately and set an actionable error, e.g.
     "Add your Gemini API key in Settings (gear icon, top-left) to start chatting." — instead of
     the generic "Couldn't reach your AI tutor…" message.
   - `app/src/main/java/com/example/viewmodel/OnboardingViewModel.kt` (`completeOnboarding` catch
     block): same treatment. Note that onboarding runs on first launch *before* the user has ever
     seen the settings screen, and the settings route is currently only reachable from the chat
     screen's top-left gear button — so either add a small "Settings" affordance on the onboarding
     screen or make the error message explain that settings become available after onboarding.
     Pick whichever is less invasive but state the decision in the summary.
   - `SettingsScreen.kt`: the Gemini "API key" field's supporting text still says
     "Leave empty to use the app's built-in key." — replace it with something like
     "Required for the Gemini API." Do **not** make blank base URL/model an error; only the key
     is required (those still fall back to defaults per `ConfigurableAiService`).

4. **Build configuration**
   - `app/build.gradle.kts`: nothing to delete here directly, but verify after the change that no
     `BuildConfig.GEMINI_API_KEY` references remain (`grep -rn "GEMINI_API_KEY" app/src`).
   - `.env.example`: delete the two `GEMINI_API_KEY` lines. Keep `AZURE_SPEECH_KEY` /
     `AZURE_SPEECH_REGION` and keep the Secrets Gradle Plugin itself — `TtsManager.kt` still uses
     `BuildConfig.AZURE_SPEECH_KEY` / `AZURE_SPEECH_REGION`.
   - `.env` (local, gitignored, never committed — verified with `git ls-files`): remove the
     `GEMINI_API_KEY` line. Remind the owner in the summary to **revoke the old key** in Google AI
     Studio, since it shipped inside previously built APKs.

5. **Docs/comments sweep**
   - Search for remaining mentions: `grep -rni "gemini_api_key\|built-in key\|bundled at build time" .`
     (excluding build outputs). Update the `ModelApiSettings` KDoc in
     `app/src/main/java/com/example/data/settings/SettingsRepository.kt`, which currently says a
     blank Gemini key means "use the key bundled at build time".

### Verification

- `./gradlew compileDebugKotlin` must pass with zero errors.
- Unit tests: `JAVA_HOME=/home/Laow/jdk-21 ./gradlew testDebugUnitTest --tests "com.example.ChatSessionTest"`
  (Robolectric needs Java 21). `GreetingScreenshotTest` is pre-existing broken — temporarily move
  it aside if it blocks compilation of the test source set, and restore it afterwards.
- `grep -rn "GEMINI_API_KEY" app/src app/build.gradle.kts .env.example` must return nothing.
- Manual sanity check to describe in the summary: with no key saved, tapping the mic should show
  the "add your key in Settings" message without a network round-trip; after saving a key in
  Settings, chatting works again.
