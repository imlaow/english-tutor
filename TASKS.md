# Development Tasks Checklist

**Agent Instruction**: Execute ONLY ONE task per session. Stop and wait for user review after completing the assigned task. DO NOT proceed to the next task automatically.

## Phase 1: Local Data Layer (User Profile)
- [ ] **Task 1**: Create the Room Database Entity (`UserProfileEntity`) and DAO (`UserDao`) inside the `data/local` package. Fields should include english_level, learning_goal, topics_of_interest, and daily_practice_time. Do not write any network or UI code.
- [ ] **Task 2**: Setup the Room Database class (`AppDatabase`) and implement the `ProfileRepository` inside the `data/repository` package. Use Manual DI concepts (no Hilt/Koin).

## Phase 2: Onboarding Logic & AI Integration
- [ ] **Task 3**: Create `OnboardingViewModel` and its corresponding UI State classes. Implement the logic to format the Gemini AI prompt, parse the returned JSON profile, and save it using `ProfileRepository`.
- [ ] **Task 4**: Connect `OnboardingViewModel` to the existing Compose UI in the `ui/` package. Ensure loading states and error handling are properly displayed on the screen.

## Phase 3: Home & Chat (To be defined later)
- [ ] **Task 5**: (Pending) Define Home screen recommendation logic.
- [ ] **Task 6**: (Pending) Implement Chat logic and Azure TTS integration via TtsManager.
