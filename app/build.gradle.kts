import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// Release signing comes from an untracked keystore.properties file (see RELEASE.md).
// Nothing secret is ever stored in this repository.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "lk.jmcinnovators.learning"
    compileSdk = 35

    defaultConfig {
        applicationId = "lk.jmcinnovators.learning"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    // AndroidX core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Compose + Material 3
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Google Sign-In through Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase (versions are managed by the BoM)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-common-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Real org.json implementation for JVM unit tests -- android.jar's org.json is a stub that
    // throws/returns defaults under Robolectric-less unit tests (see DictionaryViewModelTest).
    testImplementation("org.json:json:20231013")

    // Instrumented / UI tests
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

val ensureFirebaseConfigs by tasks.registering {
    doLast {
        val gs = file("google-services.json")
        if (!gs.exists()) {
            val template = rootProject.file("firebase/google-services.json.example")
            if (template.exists()) {
                val clean = template.readText().replace(Regex("\"// NOTE\":.*?\n"), "")
                gs.writeText(clean)
            } else {
                gs.writeText("""
                {
                  "project_info": { "project_number": "129216148625", "project_id": "jmc-home2", "storage_bucket": "jmc-home2.firebasestorage.app" },
                  "client": [ {
                    "client_info": { "mobilesdk_app_id": "1:129216148625:android:c3947477bfaef553", "android_client_info": { "package_name": "lk.jmcinnovators.learning" } },
                    "oauth_client": [ { "client_id": "129216148625-k3gkmcol114rntrmgettubri6jo9pphe.apps.googleusercontent.com", "client_type": 3 } ],
                    "api_key": [ { "current_key": "AIzaSyBqv-Ohj2QjgODRgnuOlIyXhgS82WJ_Ohs" } ],
                    "services": { "appinvite_service": {} }
                  } ],
                  "configuration_version": "1"
                }
                """.trimIndent())
            }
        }
        val rawDir = file("src/main/res/raw")
        rawDir.mkdirs()
        val classJson = file("src/main/res/raw/jmc_class_services.json")
        if (!classJson.exists()) {
            val classTemplate = rootProject.file("firebase/jmc-class-services.json.example")
            if (classTemplate.exists()) {
                val clean = classTemplate.readText().replace(Regex("\"// NOTE\":.*?\n"), "")
                classJson.writeText(clean)
            } else {
                classJson.writeText("""
                {
                  "project_info": { "project_number": "264700474615", "project_id": "jmc-class", "storage_bucket": "jmc-class.firebasestorage.app" },
                  "client": [ {
                    "client_info": { "mobilesdk_app_id": "1:264700474615:android:701719833c85d135", "android_client_info": { "package_name": "lk.jmcinnovators.learning" } },
                    "api_key": [ { "current_key": "AIzaSyBLYuM8ycUK4AYkCJaqdxZpxvLnA-Lx374" } ]
                  } ],
                  "configuration_version": "1"
                }
                """.trimIndent())
            }
        }
    }
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    dependsOn(ensureFirebaseConfigs)
}
tasks.matching { it.name.startsWith("generate") && it.name.endsWith("Resources") }.configureEach {
    dependsOn(ensureFirebaseConfigs)
}

val copyFinalApk by tasks.registering(Copy::class) {
    val src = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    from(src)
    into(rootProject.file("release"))
    rename { "JMC-Innovators-Learning-App.apk" }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(copyFinalApk)
}
