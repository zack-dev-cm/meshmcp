plugins {
    id("com.android.application") version "8.9.0" apply false
    kotlin("android") version "2.1.0" apply false
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
