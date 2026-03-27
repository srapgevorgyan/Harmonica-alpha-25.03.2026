plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.harmonica.application"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.harmonica.application"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    //androidchart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.material:material:1.9.0")

    implementation("com.google.guava:guava:31.1-android")
    implementation("androidx.core:core-splashscreen:1.0.1")

}