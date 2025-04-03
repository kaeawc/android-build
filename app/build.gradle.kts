/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.graphAssertion)
    alias(libs.plugins.publish)
    alias(libs.plugins.sortDependencies)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.zacAnvil)
}

moduleGraphAssert {
    maxHeight = 8
    allowed =
        arrayOf(
            ":app -> :.*",
            ":feature:.* -> :subsystem:.*",
            ":feature:.* -> :foundation:.*",
            ":subsystem:.* -> :foundation:.*",
            ":subsystem:.* -> :core:.*",
            ":foundation:.* -> :core:.*",
            ":core:.* -> :core:.*")
    configurations = setOf("api", "implementation")
    assertOnAnyBuild = false
}

play {
    track.set("internal")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    userFraction.set(1.0)
    defaultToAppBundles.set(true)
    serviceAccountCredentials.set(file("google-play-publishing-service-account.json"))
    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.IGNORE)
}

android {
    namespace = "dev.jasonpearson.android"
    compileSdk = libs.versions.build.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.build.android.buildTools.get()

    defaultConfig {
        applicationId = "dev.jasonpearson.android"
        minSdk = libs.versions.build.android.minSdk.get().toInt()
        targetSdk = libs.versions.build.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        named("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        /**
         * Normally we would only want to target Java 8 or 11, but since minSdk for this project is
         * as high as I want it to be I can target more recent JDK versions and use desugaring + ASM
         * to ensure older Android devices can run this just fine.
         */
        sourceCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
    }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    lint {
        sarifOutput = file("${layout.buildDirectory.get()}/reports/lint-results.sarif")
        htmlOutput = file("${layout.buildDirectory.get()}/reports/lint-results.html")
        abortOnError = true
        checkDependencies = true
        ignoreTestSources = true

        // Detect command line arguments
        val customLintConfig = project.findProperty("lint-config") as? String ?: "default"

        val lintConfigFile = file("../android-lint/${customLintConfig}-lint.xml")
        if (!lintConfigFile.exists()) {
            throw GradleException("Lint config file not found: ${lintConfigFile.absolutePath}")
        } else {
            println("Using ${lintConfigFile.absolutePath} for lint config")
        }

        lintConfig = lintConfigFile
    }
}

anvil {
    useKsp(
        contributesAndFactoryGeneration = true,
        componentMerging = true,
    )
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    // Needed for reading Java 19+ class files due to JVM target higher than 11
    implementation(platform(libs.asm.bom))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.kotlin)

    implementation(libs.dagger.runtime)
    ksp(libs.dagger.compiler)

    implementation(libs.zacAnvil.annotations)
    implementation(libs.zacAnvil.annotationsOptional)
    ksp(libs.zacAnvil.compiler)

    kspAndroidTest(libs.zacAnvil.compiler)

    debugImplementation(libs.bundles.compose.ui.debug)

    testImplementation(libs.bundles.unit.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.ui.espresso.test)
    implementation(libs.navigation.compose)
}
