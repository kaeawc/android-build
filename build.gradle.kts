plugins {
    `version-catalog`
}

buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.agp)
        classpath(libs.kgp)
    }
}
