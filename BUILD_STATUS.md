# Build Status

The project is configured as a native Kotlin/Jetpack Compose Android application.

## Verified in this package

- No Android WebView is used by the native source.
- `functions/index.js` passes `node --check`.
- `firebase/classroom/functions/index.js` passes `node --check`.
- `google-services.json` contains an Android client for `lk.jmcinnovators.learning`.
- Gradle configuration and Android source are included.

## Important environment note

The uploaded source package did not contain `gradle/wrapper/gradle-wrapper.jar`, and the build environment used to prepare this package has no network access to download Gradle 8.9. Therefore a full `assembleDebug` build could not be executed here.

When opening the project in Android Studio, allow Gradle sync to restore the configured Gradle 8.9 wrapper, or run `gradle wrapper --gradle-version 8.9` on a machine with Gradle installed. Then run:

```bash
./gradlew clean assembleDebug
```

The wrapper script has been corrected so JVM options are passed correctly once the wrapper JAR is present.
