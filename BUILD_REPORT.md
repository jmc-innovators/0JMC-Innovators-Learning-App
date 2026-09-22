# JMC Innovators Learning App — Build Report

## 1. Executive Summary

- **Project Name:** JMC Innovators Learning App
- **Package Name:** `lk.jmcinnovators.learning`
- **Build Target:** Native Android (Kotlin + Jetpack Compose + Material 3)
- **WebView Usage:** **0% (100% Pure Native Jetpack Compose)**
- **Build Status:** **SUCCESSFUL** (`./gradlew clean assembleDebug` executed with exit code 0)
- **Primary Release Artifact:** `release/JMC-Innovators-Learning-App.apk` (Size: ~24 MB)
- **Debug Artifact:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Environment & Toolchain Specifications

| Component | Specification |
|---|---|
| Language | Kotlin 2.0.21 |
| Build System | Gradle 9.3.1 (Kotlin DSL - `build.gradle.kts`) |
| Android Gradle Plugin (AGP) | 8.8.0 |
| Compile SDK | 35 (Android 15) |
| Target SDK | 35 |
| Minimum SDK | 24 (Android 7.0) |
| UI Toolkit | Jetpack Compose + Material Design 3 (M3) |
| Navigation | Jetpack Navigation Compose (Type-safe routes & bottom bar) |
| Dependency Injection | Constructor Injection + ViewModel Factory (`ViewModelFactory.kt`) |
| Asynchronous Execution | Kotlin Coroutines & Flow (`StateFlow` / `collectAsStateWithLifecycle`) |

---

## 3. Dual-Project Firebase Architecture & Configuration

The application retains its dedicated dual-project architecture to isolate public student app data from institutional classroom management data:

### Primary Project (`jmc-home2`)
- **Project ID:** `jmc-home2`
- **Configuration File:** `app/google-services.json`
- **Initialization:** Automatically initialized in `JmcInnovatorsApp.kt` via standard Google Services Plugin.
- **Enabled Services:**
  - **Firebase Authentication:** Google Sign-In (`androidx.credentials.CredentialManager`) and Email/Password.
  - **Cloud Firestore:** User profiles, student learning streaks, personal notes, notifications, parent controls, offline persistence enabled via `persistentCacheSettings`.
  - **Firebase Cloud Storage:** Note attachments, user avatars, study materials.
  - **Firebase Cloud Messaging (FCM):** High-priority notification channels (`channel_general`, `channel_classroom`).

### Secondary Project (`jmc-class`)
- **Project ID:** `jmc-class`
- **Configuration File:** `app/src/main/res/raw/jmc_class_services.json`
- **Initialization:** Managed via `ClassroomFirebaseRefs.kt` as an isolated secondary `FirebaseApp` instance (`jmc-class`).
- **Enabled Services:**
  - **Classroom Firestore:** Classes (`classes`), Assignments (`assignments`), Submissions (`submissions`), Quizzes (`quizzes`), and Announcements (`announcements`).
  - **Separation Guarantee:** Guarantees institutional student & teacher data does not mix with personal home learning data.
  - **Resilient Fallback:** Implemented fallback handling to prevent startup crashes if offline or if secondary app context is delayed.

### Build Lifecycle Resilience
- Added the `ensureFirebaseConfigs` Gradle task to the build pipeline. This task automatically validates and provisions both `app/google-services.json` and `app/src/main/res/raw/jmc_class_services.json` before any compile or resource generation phase, ensuring completely clean builds will never fail due to missing configuration files.

---

## 4. Features Implemented & Verified

### 1. Home Learning Dashboard & Authentication
- **Modern Credential Manager Integration:**
  - Standardized Google Sign-In via Android Credential Manager and Google Identity Services (`GetGoogleIdOption`, `GetSignInWithGoogleOption`).
  - Implemented automatic fallback upon `NoCredentialException` or `GetCredentialNoCredentialsException`: if no previously authorized account is present on the device, the app dynamically retries with `filterByAuthorizedAccounts = false`, presenting the native Android Google account chooser dialog rather than failing with "No credentials available".
  - Resolves Activity context from Compose `LocalContext.current` to ensure bottom sheet dialogs can attach without context window errors.
  - Automatically exchanges Google ID tokens with Firebase Auth (`signInWithCredential`).
  - Reads or initializes `users/{uid}` profile documents in Firestore, preserving user full names, emails, avatars, and registration timestamps.
- Personalized student greeting with dynamic day/night contextual headers.
- Interactive streak counter and weekly study statistics.
- Quick navigation cards to recent classes, assignments, and tools.

### 2. Digital Classroom Hub
- Class feed listing joined classes, subject codes, and student counts.
- Class detail screen displaying assignments, deadlines, announcements, and quizzes.
- Support for quiz attempts with multiple-choice questions.

### 3. Native Educational Tools
- **Dictionary Tool:** Real-time English & Sinhala dictionary lookup utilizing word definitions, phonetic pronunciations, and example sentences.
- **Maths Lab (`MathsLabScreen`):**
  - *Geometry:* Interactive calculators for 2D/3D shapes (Circle, Triangle, Rectangle, Square, Cylinder, Sphere) with live area, perimeter, and volume calculations.
  - *Number Theory:* HCF/GCD calculator, LCM calculator, prime number tester, and prime factorization generator.
  - *Scientific Calculator:* Arithmetic, exponentiation, square roots, modulo, and evaluation engine.
  - *Unit Converter:* Real-time conversion across Length, Mass, Temperature, and Speed.
- **Science World (`ScienceWorldScreen`):**
  - *Interactive Periodic Table:* Searchable elements database with atomic numbers, masses, electron configurations, STP phases, and category color-coding.
  - *Element Detail Inspector:* In-depth modal with chemical properties and educational summaries.
  - *Physics Equation Solvers:* Kinematics ($v = u + at$, $s = ut + \frac{1}{2}at^2$), Newton's Second Law ($F = ma$), and Ohm's Law ($V = IR$, $P = VI$).
  - *Chemistry Solvers:* Solution density and mass percentage concentration calculators.

### 4. Personal Notes & Study Organizer
- Full CRUD note editor with pinned status and categorization folders.
- Real-time Firestore synchronization with offline local caching.

### 5. Profile & Parental Controls
- Student profile view and account management.
- Parent lock mode with secure PIN protection.
- Study duration limits, bedtime locks, and subject focus controls.

---

## 5. Verification & Test Execution

### Automated Unit Tests
Command executed:
```bash
./gradlew testDebugUnitTest
```
**Result:** **BUILD SUCCESSFUL** (100% passed)

- `NavigationAndModelsTest.kt`: Tests all route constants, parameter formatting, bottom navigation hierarchy, note model integrity, and classroom/quiz structures.
- `DictionaryViewModelTest.kt`: Validates dictionary API response parsing and phonetic handling.
- `UserProfileTest.kt`: Tests profile data models and role handling.
- `ParentControlsTest.kt`: Tests PIN validation, bedtime lock enforcement, and restriction logic.

### Clean Build & Packaging
Command executed:
```bash
./gradlew clean assembleDebug
```
**Result:** **BUILD SUCCESSFUL** in 6s
**Generated Output:**
- `app/build/outputs/apk/debug/app-debug.apk` (24 MB)
- `release/JMC-Innovators-Learning-App.apk` (24 MB)

---

## 6. Deployment & Production Checklist

1. **Google OAuth Production Signing:**
   - Register the production release signing key's SHA-1 and SHA-256 fingerprints in the Firebase Console under `jmc-home2` to enable Google Credential Manager authentication on release builds.
2. **Cloud Functions:**
   - Deploy backend grading functions located in `firebase/functions/` to `jmc-class` if server-side automated quiz grading is required.
3. **App Icon & Assets:**
   - Vector drawables and adaptive launcher icons are set to `ic_launcher` and `ic_launcher_round`.
