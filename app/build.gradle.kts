val appVersion = "0.4.1"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.tau.studymate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tau.studymate"
        minSdk = 26
        // Play requires 36; 37 changes runtime behavior, so it gets its own tested release
        //noinspection OldTargetApi
        targetSdk = 36
        // Bump appVersion for every Play upload; versionCode follows it (0.0.1 -> 1, 0.1.0 -> 100, 1.2.3 -> 10203)
        versionName = appVersion
        versionCode = appVersion.split(".").map(String::toInt).let { (a, b, c) -> a * 10000 + b * 100 + c }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // CI has the keystore. Locally (no keystore) fall back to the debug key so tooling such as the
            // Baseline Profile generator can install the build; Play rejects such an upload (wrong key).
            // Android Studio's "Generate Signed App Bundle" signs with the real key either way.
            signingConfig = if (file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks").exists())
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Release-like build signed with the debug key, for measuring real performance (debug Compose is several times slower)
    buildTypes.create("benchmark") {
        initWith(buildTypes.getByName("release"))
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += "release"
        isDebuggable = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// Room schema history; used by the migration test (app/src/androidTest)
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
android.sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

// Output files: TAU-StudyMate-v0.0.1-release.aab / .apk
base { archivesName.set("TAU-StudyMate-v$appVersion") }

// Profile is merged into src/main so every release ships it: ./gradlew :app:generateBaselineProfile
baselineProfile { mergeIntoMain = true }


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.mlkit.translate)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.play.review.ktx)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.pdf.document.service)
    implementation(libs.kotlinx.serialization.core)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
