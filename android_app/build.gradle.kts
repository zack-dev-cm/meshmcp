plugins {
    id("com.android.application") version "8.2.0" apply false
    kotlin("android") version "1.9.22" apply false
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
