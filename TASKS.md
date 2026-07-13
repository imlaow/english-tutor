# Current Active Task

**Agent Instruction**: You are allowed to read this file and ARCHITECTURE.md only. You must execute ONLY the task listed below. Once finished, stop immediately and wait for user review. DO NOT create, plan, or execute any other tasks.

- [ ] **Task 3**: Create the `OnboardingViewModel` and its corresponding UI State classes inside the `viewmodel/` package. 
  - Implement the logic to format the system prompt for the Google Gemini API.
  - Implement a function to request the AI, parse the returned JSON user profile, and save it to the database using `ProfileRepository`. 
  - Use Manual DI (ViewModelProvider.Factory) to inject the repository into the ViewModel. 
  - DO NOT connect it to the Compose UI yet.
  - 