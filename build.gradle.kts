// Versions live here and in app/build.gradle.kts rather than in a version
// catalog: on this machine Gradle 9.3.1 failed to generate the `libs`
// accessors (see README), and two files are easy enough to keep in sync.
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
}
