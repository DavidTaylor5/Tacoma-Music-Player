import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.andaagii.tacomamusicplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.andaagii.tacomamusicplayer"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "1.5"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
//    kapt("groupId:artifactId:version")


    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    //ROOM
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

////    // To use Kotlin Symbol Processing (KSP) This isn't building / syncing...
    ksp("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // Moshi for Json
    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //Timber logs
    implementation("com.jakewharton.timber:timber:4.7.1")

    //media3, all modules need to be the same version
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")

    //adding viewmodel functionality
    implementation ("androidx.activity:activity-ktx:1.8.2")
    implementation ("androidx.fragment:fragment-ktx:1.6.2")

    //Add Navigation Component to Project
    val navigationVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //Coil
    implementation("io.coil-kt:coil:2.5.0")
}