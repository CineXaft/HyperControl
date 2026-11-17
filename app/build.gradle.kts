plugins { id("com.android.application") }

android {
    namespace = "dev.local.hypercontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cinexaft.hypercontrol"
        minSdk = 35
        // The original APK metadata reports target 37; keep it and use a matching SDK for releases.
        targetSdk = 37
        versionCode = 10408
        versionName = "1.4.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].resources.srcDir("src/main/resources")
}

dependencies {
    // The module is injected into the Settings process; AndroidX and framework classes are
    // supplied by com.android.settings at runtime, exactly as in the original 73 KB APK.
    compileOnly("androidx.appcompat:appcompat:1.7.0")
    compileOnly("androidx.fragment:fragment:1.8.5")
    compileOnly("androidx.preference:preference:1.2.1")
    compileOnly("io.github.libxposed:api:102.0.0")
    compileOnly(project(":device-stubs"))
}
