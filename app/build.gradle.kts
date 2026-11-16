plugins { id("com.android.application") }

android {
    namespace = "dev.local.hypercontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cinexaft.hypercontrol"
        minSdk = 35
        // APK metadata reports target 37. Set compile SDK 37 before attempting a release rebuild.
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
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.preference:preference:1.2.1")
    compileOnly("io.github.libxposed:api:102.0.0")
    compileOnly(project(":device-stubs"))
}
