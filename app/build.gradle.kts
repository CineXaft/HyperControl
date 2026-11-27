plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cinexaft.hypercontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cinexaft.hypercontrol"
        minSdk = 35
        ndk { abiFilters += "arm64-v8a" }
        // Note: SDK 37 not yet available; using 35 for now
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-202601127"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        // Keep DEX deflated; Android requires resources.arsc to remain Stored.
        dex { useLegacyPackaging = true }
        jniLibs { useLegacyPackaging = true }
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
    // Injected into the Settings process: AndroidX and framework classes come from
    // com.android.settings at runtime, exactly like the original 73 KB APK.
    compileOnly("androidx.appcompat:appcompat:1.7.0")
    compileOnly("androidx.fragment:fragment:1.8.5")
    compileOnly("androidx.preference:preference:1.2.1")
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("org.luckypray:dexkit:2.3.0")
    compileOnly(project(":device-stubs"))
}
