# FIREBASE_SETUP.md

This app talks to **two** Firebase projects. See MIGRATION.md for why they're
kept separate.

## 1. jmc-home2 — accounts, profile, notes, notifications

1. Firebase console → project **jmc-home2**.
2. Project settings → Your apps → Android. If `lk.jmcinnovators.learning` isn't
   registered yet, add it (package name must match `app/build.gradle.kts`
   `applicationId`, currently `lk.jmcinnovators.learning`). If you'd rather keep
   the original package `com.jmcinnovators.app` from the ZIP you supplied, that
   registration already exists — change `applicationId`/`namespace` in
   `app/build.gradle.kts` to match instead of re-registering.
3. Download `google-services.json`, place it at `app/google-services.json`.
4. Add your debug (and later release) SHA-1 — see SETUP.md's Google Sign-In
   section — then re-download the file.
5. **Update `app/src/main/res/values/strings.xml`**'s `default_web_client_id` to
   match the `"client_type": 3` OAuth client in your downloaded file. It's
   currently set to the value from the config you supplied
   (`129216148625-k3gkmcol114rntrmgettubri6jo9pphe.apps.googleusercontent.com`);
   if you re-create the Firebase project or add a new OAuth client this value
   changes and Google Sign-In will fail silently until it's updated.
6. Deploy security rules and functions:
   ```
   firebase deploy --only firestore:rules,storage:rules --project jmc-home2
   cd functions && npm install && firebase deploy --only functions --project jmc-home2
   ```
   Rules are at `firebase/firestore.rules` / `firebase/storage.rules`.

## 2. jmc-class — schools, classes, assignments, quizzes

1. Firebase console → project **jmc-class**.
2. Register the same Android package there too (an app can be registered in
   more than one Firebase project — this one is read at runtime via a
   *secondary* `FirebaseApp`, see `ClassroomFirebaseRefs.kt`).
3. Download that project's `google-services.json` and save it as
   `app/src/main/res/raw/jmc_class_services.json` (note: lowercase, `.json`
   extension — Android resource names are case-sensitive and can't contain
   dashes).
4. Deploy:
   ```
   firebase deploy --only firestore:rules,storage:rules --project jmc-class
   cd firebase/classroom/functions && npm install && firebase deploy --only functions --project jmc-class
   ```
   Rules are at `firebase/classroom/firestore.rules` / `firebase/classroom/storage.rules`.

## Firestore structure

See the tables in [MIGRATION.md](../MIGRATION.md) and the model classes in
`app/src/main/java/lk/jmcinnovators/learning/data/model/` — every field name
was written to match what `legacy-web/` already reads and writes, not invented
fresh.

## Rotate these before going further

Found hardcoded in the website source during this migration — not introduced
by this app, but worth fixing regardless of whether you ship the native app:

- `OP_KEY` in `legacy-web/jmc_Classroom.html`
- `ADMIN_PASS` in `legacy-web/notebook.html`
- The Android release keystore password that shipped in plaintext in the
  original project ZIP's `gradle.properties` (see RELEASE.md — this app's
  keystore is no longer committed to the repo at all).
