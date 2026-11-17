plugins { id("com.android.application") }

android {
    namespace = "com.cinexaft.hypercontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cinexaft.hypercontrol"
        minSdk = 35
        // The original APK metadata reports target 37; keep it and use a matching SDK for releases.
        targetSdk = 37
        versionCode = 10408
        versionName = "1.4.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Strings are resolved by name at runtime; res/raw/keep.xml protects them.
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        // The original module ships classes.dex deflated. AGP 8 stores it uncompressed by default,
        // which bloats the APK; restore the original legacy layout. resources.arsc must stay
        // uncompressed for Android 11+ (API 30) and is left Stored by AAPT2.
        dex {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].resources.srcDir("src/main/resources")
}

dependencies {
    // Injected into the Settings process: AndroidX and framework classes come from
    // com.android.settings at runtime, exactly like the original 73 KB APK.
    compileOnly("androidx.appcompat:appcompat:1.7.0")
    compileOnly("androidx.fragment:fragment:1.8.5")
    compileOnly("androidx.preference:preference:1.2.1")
    compileOnly("io.github.libxposed:api:102.0.0")
    compileOnly(project(":device-stubs"))
}
