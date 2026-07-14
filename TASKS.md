# Current Active Task

**Agent Instruction**: You are allowed to read this file and ARCHITECTURE.md only. You must execute ONLY the task listed below. Once finished, stop immediately and wait for user review. DO NOT create, plan, or execute any other tasks.

- [ ] **Task 5**: Replace the mock reply logic in `ChatViewModel.onSpeechResult` with a real Gemini integration.
  - Inject `ProfileRepository` and `GeminiApiService` into `ChatViewModel` via a manual factory (same pattern as `OnboardingViewModelFactory`; no Hilt/Koin) and update `MainActivity` to use it.
  - On each speech result, call `GeminiApiService.generateContent` with a tutor system prompt and the user's utterance, requesting `application/json` output with exactly this schema: `{"ai_response": "...", "grammar_correction": "... or null"}`.
  - Load the saved `UserProfileEntity` from Room and include it in the system prompt (English level, learning goal, topics of interest, daily practice time) so replies are personalized to the learner.
  - Include recent conversation turns from the in-memory history in the request so the dialogue stays coherent.
  - Parse the JSON response into `ChatMessage` (`aiResponse`, `grammarCorrection` = null when there is no correction) and keep the existing history/speakEvent/isProcessing flow unchanged.
  - Handle API failures by setting the existing `error` StateFlow with a user-readable message and clearing `isProcessing`; never crash and never add a fake message to history.
  - Remove the `delay(1500)` mock and the length-parity grammar correction entirely.
  - DO NOT modify `OnboardingViewModel`, `ProfileRepository`, or the Compose UI beyond wiring the new factory.
