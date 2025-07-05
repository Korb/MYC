plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "v4lpt.vpt.f023.MYC"
    compileSdk = 34

    defaultConfig {
        applicationId = "v4lpt.vpt.f023.MYC"
        minSdk = 33
        targetSdk = 34
        versionCode = 1003
        versionName = "1.0.3"
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Correct property name
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

}