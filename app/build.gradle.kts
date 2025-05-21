plugins {
    id("com.android.application")
}

android {
    namespace = "com.hichemtabtech.controldcmotor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hichemtabtech.controldcmotor"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.0")
    implementation("androidx.navigation:navigation-fragment:2.9.0")
    implementation("androidx.navigation:navigation-ui:2.9.0")

    // Splash screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // MPAndroidChart for real-time charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.github.yukuku:ambilwarna:2.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
