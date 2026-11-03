import java.util.Properties

// AGP 9 compiles Kotlin itself, so there's no separate Kotlin plugin.
plugins {
    id("com.android.application")
}

// Release signing details live outside git. Every release must use the same key, or phones
// refuse to install the update over the old version.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.cocwidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cocwidget"
        minSdk = 31
        targetSdk = 35
        versionCode = 15
        versionName = "2.2"
    }

    signingConfigs {
        create("release") {
            if (!keystoreProperties.isEmpty) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Some tests read these files straight from disk. Without declaring them, Gradle treats the
// tests as up to date after a layout or game data change and silently skips them.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/res/layout").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/assets/coc_data.json").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // android.jar only has stubs of org.json, so JVM tests need the real library.
    testImplementation("org.json:json:20240303")
}
