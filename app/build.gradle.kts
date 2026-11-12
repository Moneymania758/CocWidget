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
        versionCode = 42
        versionName = "2.15"
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

    // Lets the screen tests in the acceptance package load real layouts and strings.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Some tests read these files straight from disk. Without declaring them, Gradle treats the
// tests as up to date after a layout or game data change and silently skips them.
tasks.withType<Test>().configureEach {
    // The screen tests open hundreds of real pages, more than the default 512 MB holds.
    maxHeapSize = "2g"

    // Screens start icon downloads and update checks in the background. In a test they'd be slow and
    // flaky, and a half-written icon can reach a later test. A dead proxy makes them fail at once, as
    // they do offline. Only Robolectric's first download of its Android runtime gets through.
    for (scheme in listOf("http", "https")) {
        systemProperty("$scheme.proxyHost", "127.0.0.1")
        systemProperty("$scheme.proxyPort", "9")
    }
    systemProperty("http.nonProxyHosts", "repo1.maven.org")

    inputs.dir("src/main/res/layout").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/res/values/strings.xml").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/assets/coc_data.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/assets/guide_content.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/assets/guide_facts.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/assets/guide_units.json").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // android.jar only has stubs of org.json, so JVM tests need the real library.
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.17")
}
