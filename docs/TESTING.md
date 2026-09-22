# TESTING.md

## What actually ran

**Nothing has been compiled or executed.** The environment this project was
written in has no Android SDK and no network access to
`dl.google.com`/`repo.maven.apache.org`/`services.gradle.org` (Gradle itself
couldn't even be downloaded). Every file here was hand-written and reviewed,
not built. Treat the first `./gradlew assembleDebug` you run as the first real
compiler check this code has ever had — see the note in `README.md`.

## What to run, and in what order, once you have Android Studio

```
./gradlew clean
./gradlew test                # unit tests, see below
./gradlew lint                # abortOnError = true in app/build.gradle.kts,
                               # so a real lint error fails this step
./gradlew assembleDebug       # confirms the APK actually links
```

If any step fails, it's most likely one of these (in rough order of
likelihood, given how this was written without a compiler):

1. A Compose import path that moved between library versions — every
   dependency version in `app/build.gradle.kts` matches what Firebase's
   console gave you (BoM 34.19.0) or a version current as of early 2026; double
   check `androidx.compose:compose-bom` and `androidx.navigation:navigation-compose`
   against the versions Android Studio's upgrade assistant suggests.
2. `default_web_client_id` in `strings.xml` not matching your actual
   `google-services.json` — see FIREBASE_SETUP.md step 5.
3. A missing `res/raw/jmc_class_services.json` — the Classroom tab is the only
   thing that touches this at runtime, so everything else in the app will
   build and run fine without it; it'll only surface when you open Classroom.

## Unit tests included

`app/src/test/java/lk/jmcinnovators/learning/`:

- `DictionaryViewModelTest.kt` — JSON-parsing of a sample dictionaryapi.dev
  response into `DictionaryEntry`/`DictionaryMeaning`, independent of network
  access.
- `UserProfileTest.kt` — `isTeacherOrAbove` role logic.
- `ParentControlsTest.kt` — `isFeatureEnabled` / `emergencyLock` precedence,
  since this gates what a student can access and needs to be right.

These are the tests that could be written without a device, emulator, or live
Firestore — i.e., pure logic with no Android framework or Firebase SDK call in
the path. Everything else (repositories, ViewModels that touch Firebase,
Compose screens, navigation, the actual classroom-join flow) needs either
Robolectric, an instrumented device/emulator, or the Firebase Local Emulator
Suite to test meaningfully — none of which are available in this environment.
**Adding that coverage is real remaining work**, not something to assume is
covered by the three tests above.

## Manually verify before you consider this "working"

Once it builds and runs on a device:

- [ ] Onboarding shows once, not on second launch (DataStore persists)
- [ ] Google Sign-In completes (requires the SHA-1 step in SETUP.md)
- [ ] A brand-new account lands on Profile Setup, not Home
- [ ] Profile Setup write shows up in the jmc-home2 Firestore console under
      `users/{uid}` with the fields you entered
- [ ] Sign out, sign back in with the same account → lands on Home directly
- [ ] Classroom → Join with a made-up code → "Classroom code not found."
      (requires at least one real `classes/{id}` document with a `joinCode`
      field in jmc-class to test the success path)
- [ ] Create a note, kill and reopen the app → note is still there
      (Firestore offline cache) and syncs (check the jmc-home2 console)
- [ ] Turn on airplane mode → offline banner appears; notes/profile still
      readable from cache
- [ ] Dark/Light/System theme switch actually changes the UI
