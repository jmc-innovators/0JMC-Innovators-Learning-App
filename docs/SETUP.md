# SETUP.md

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17 (bundled with recent Android Studio)
- An Android device or emulator running API 24+

## One file this repo could NOT include

`gradle/wrapper/gradle-wrapper.jar` is missing — the environment this project
was written in has no network access to `services.gradle.org`, and that jar is
a binary Gradle distributes, not something safe to hand-write. `gradlew` /
`gradlew.bat` are here and will work once the jar exists. Fix it with either:

- **Easiest:** just open the project in Android Studio. It detects the missing
  wrapper and offers to regenerate it, or you can use its bundled Gradle
  directly (Android Studio doesn't strictly need `gradlew` to sync/build).
- **From the command line**, if you have any Gradle installed locally:
  ```
  gradle wrapper --gradle-version 8.9
  ```
  This creates the jar and confirms the properties file already here match.

## First build checklist

The project will **not** build or run until these two things are in place —
both are deliberately excluded from the repository (see `.gitignore`):

1. **`app/google-services.json`** — the jmc-home2 config. Get it from Firebase
   console → jmc-home2 → Project settings → your Android app. See
   [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
2. **`app/src/main/res/raw/jmc_class_services.json`** — the jmc-class config,
   same process, different project. Also in FIREBASE_SETUP.md.

Without step 1, the app crashes on launch (`FirebaseApp.initializeApp` has
nothing to read). Without step 2, everything except the Classroom tab works —
`ClassroomFirebaseRefs` will throw when it's first touched.

## Opening the project

1. `Open` the repository root in Android Studio (the folder containing
   `settings.gradle.kts`).
2. Let Gradle sync. First sync downloads the Gradle 8.9 distribution and all
   dependencies — needs network access to `dl.google.com`,
   `repo.maven.apache.org`, and `services.gradle.org`.
3. Drop in the two config files above.
4. Run `app` on a device or emulator (Run ▸ Run 'app', or `Shift+F10`).

## Command line

```
./gradlew clean
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Google Sign-In will fail until you do this

Google Sign-In needs your build's SHA-1 fingerprint registered against the
jmc-home2 Android app in the Firebase console. Get it with:

```
./gradlew signingReport
```

Copy the `SHA1` line under `Variant: debug`, paste it into Firebase console →
Project settings → your Android app → Add fingerprint, then **re-download**
`google-services.json` (it changes once a fingerprint is added) and replace
`app/google-services.json`. Repeat with your release keystore's SHA-1 before
you ship a release build — see [RELEASE.md](RELEASE.md).
