plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun prop(name: String): String = providers.gradleProperty(name).get()
fun optionalProp(name: String): String? = providers.gradleProperty(name).orNull

// Release signing comes from the environment (GitHub Actions secrets) and is
// never committed. Without it, `assembleRelease` still builds, just unsigned.
val keystorePath: String? = System.getenv("NOVA_KEYSTORE_PATH")

android {
    namespace = "pe.com.novasys.asistencia"
    compileSdk = 37

    defaultConfig {
        applicationId = "pe.com.novasys.asistencia"
        minSdk = 26
        targetSdk = 37
        // The release workflow passes these from the git tag (v0.2.0) and the
        // run number; local builds fall back to the values below.
        versionCode = optionalProp("novaVersionCode")?.toInt() ?: 1
        versionName = optionalProp("novaVersionName") ?: "0.2.0"

        buildConfigField("String", "COGNITO_REGION", "\"${prop("novaCognitoRegion")}\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"${prop("novaCognitoClientId")}\"")
        buildConfigField("String", "UPDATE_REPO", "\"${prop("novaUpdateRepo")}\"")
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("NOVA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NOVA_KEY_ALIAS")
                keyPassword = System.getenv("NOVA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_BASE_URL", "\"${prop("novaApiBaseUrlDebug")}\"")
            // Debug builds are installed by hand; only releases self-update.
            buildConfigField("boolean", "UPDATES_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "API_BASE_URL", "\"${prop("novaApiBaseUrl")}\"")
            buildConfigField("boolean", "UPDATES_ENABLED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val lifecycle = "2.11.0"

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Backdrop blur for the liquid-glass surfaces (real blur on Android 12+).
    implementation("dev.chrisbanes.haze:haze:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
}
