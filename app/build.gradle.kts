val appVersion = "0.1.4"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tau.studymate"
    //noinspection GradleDependency
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tau.studymate"
        minSdk = 26
        // 37 needs the new SDK installed and is not required by Play yet
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
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
android.sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

// Output files: TAU-StudyMate-v0.0.1-release.aab / .apk
base { archivesName.set("TAU-StudyMate-v$appVersion") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta5")
    implementation("com.google.mlkit:translate:17.0.3")
    // room-testing needs serialization 1.8+; the app classpath (which androidTest is aligned to) pins 1.6.3 otherwise
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.room:room-testing:${libs.versions.room.get()}")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
