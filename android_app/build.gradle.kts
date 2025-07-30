plugins {
    id("com.android.application") version "8.7.3" apply false
    kotlin("android")               version "2.1.0" apply false
    // NEW ─ make the Compose compiler available to modules that need it
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
