# RELEASE.md

## Signing

No keystore is committed to this repository — the ZIP originally supplied
contained one (`jmc_release.jks`) with its password in plaintext in
`gradle.properties`. That's treated as compromised (see MIGRATION.md); **do not
reuse it.**

`app/build.gradle.kts` reads signing config from a `keystore.properties` file
at the repo root, which is git-ignored. Create your own:

1. Generate a new keystore:
   ```
   keytool -genkeypair -v -keystore jmc-release.jks -alias jmc \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   Store `jmc-release.jks` outside the repo, or in the repo root if you accept
   it staying local-only (it's git-ignored either way).
2. Create `keystore.properties` in the repo root:
   ```
   storeFile=../jmc-release.jks
   storePassword=CHANGE_ME
   keyAlias=jmc
   keyPassword=CHANGE_ME
   ```
   (Path is relative to `app/`, since that's where `rootProject.file(...)` in
   `app/build.gradle.kts` resolves it from — adjust if you place the `.jks`
   elsewhere.)
3. Register this new keystore's SHA-1 (and SHA-256, for App Bundle uploads via
   Play App Signing) in the jmc-home2 Firebase console, same as the debug one —
   see SETUP.md. Without this, Google Sign-In fails in release builds even
   though it worked in debug.

If you already have this app live on Play with the original keystore, don't
regenerate it — instead reset your upload key through Play App Signing, which
keeps your existing app listing intact. Google's own guide:
https://support.google.com/googleplay/android-developer/answer/9842756

## Building a release

```
./gradlew bundleRelease   # .aab for Play Store upload — preferred
./gradlew assembleRelease # .apk, for direct distribution/testing
```

Output: `app/build/outputs/bundle/release/app-release.aab` or
`app/build/outputs/apk/release/app-release.apk`.

`isMinifyEnabled` and `isShrinkResources` are on for release; `proguard-rules.pro`
currently only keeps annotations/line numbers, since none of the Firestore
model classes need reflection-based keep rules (Firestore's Kotlin
serialization uses generated field access, not runtime reflection, for data
classes with default values — verify this still holds if you add a model class
that Firestore struggles to deserialize after shrinking, and add a `-keep`
rule for that specific class).

## Before publishing

- [ ] Both `google-services.json` files are the real ones, not the
      `.example` placeholders (`firebase/*.example`)
- [ ] Release SHA-1/SHA-256 registered in Firebase console (jmc-home2)
- [ ] `versionCode`/`versionName` bumped in `app/build.gradle.kts`
- [ ] Rotated the `OP_KEY` / `ADMIN_PASS` secrets found in `legacy-web/` (see
      MIGRATION.md) — they're a pre-existing exposure independent of this app,
      but should be fixed regardless
- [ ] Firestore/Storage rules deployed to **both** projects (FIREBASE_SETUP.md)
