import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.andaagii.tacomamusicplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.andaagii.tacomamusicplayer"
        minSdk = 30
        targetSdk = 36
        versionCode = 201
        versionName = "v2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        outputs.all {
            // Cast to ApkVariantOutputImpl to access outputFileName
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl

            val appName = "tacoma_music_player"
            val version = versionName
            val buildType = buildType.name

            val currentDate = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd")
            val formattedDate = currentDate.format(formatter)

            output.outputFileName = "${appName}_${version}_${buildType}_${formattedDate}.apk"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.property("RELEASE_STORE_FILE") as String)
            storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        debug {

        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlinOptions {
        jvmTarget = "18"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

dependencies {

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Moshi for JSON + Retrofit
    implementation(libs.squareup.moshi)
    implementation(libs.squareup.retrofit)
    ksp(libs.squareup.moshi.kotlin.codegen)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)

    // Timber logs
    implementation(libs.timber)

    // mp3agic
    implementation(libs.mp3agic)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Allows await for MediaBrowser.buildAsync()
    implementation(libs.androidx.concurrent.futures.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Hilt WorkManager integration
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Media3 — all modules must stay on the same version
    // Note: kept at 1.2.1; 1.4.1+ does not show album art on the notification
    implementation(libs.bundles.media3)

    // ViewModel / Fragment extensions
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Navigation Component
    implementation(libs.bundles.navigation)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.espresso.core)

    // Coil — image loading
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // uCrop — image cropping
    implementation(libs.ucrop)

    // Jetpack Compose — BOM pins all compose.* versions together
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}