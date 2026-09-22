// Top-level build file. Plugin versions live here; modules apply them without versions.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Google services Gradle plugin (reads app/google-services.json)
    id("com.google.gms.google-services") version "4.5.0" apply false
}
