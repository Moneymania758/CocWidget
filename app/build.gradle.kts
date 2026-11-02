// AGP 9 compiles Kotlin itself, so there's no separate Kotlin plugin.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.cocwidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cocwidget"
        minSdk = 31
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
