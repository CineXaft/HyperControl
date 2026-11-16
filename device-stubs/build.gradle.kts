plugins { id("com.android.library") }

android {
    namespace = "reconstruction.device.stubs"
    compileSdk = 35
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("androidx.fragment:fragment:1.8.5")
    compileOnly("androidx.preference:preference:1.2.1")
    compileOnly("androidx.appcompat:appcompat:1.7.0")
}
