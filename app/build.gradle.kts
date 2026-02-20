plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
}

android {
    namespace = "com.bookshlef.bookshelf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bookshlef.bookshelf"
        minSdk = 23
        targetSdk = 35
        versionCode = 8
        versionName = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("/Users/A-201145/Documents/Playstore/PlaystoreKey.jks")
            storePassword = "Nikbook@1"
            keyAlias = "playstorekey"
            keyPassword = "Nikbook@1"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // ZXing embedded scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Networking - Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Image loading
    implementation("io.coil-kt:coil:2.6.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-common-jvm:2.8.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")


    ksp("androidx.room:room-compiler:2.6.1")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")



}