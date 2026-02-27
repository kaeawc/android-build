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
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        // Uncomment to pin R8 version from the R8 releases repo
        // exclusiveContent {
        //     forRepository {
        //         maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
        //     }
        //     filter { includeModule("com.android.tools", "r8") }
        // }
    }
}

dependencyResolutionManagement {
    repositories {
        // Local Maven repository for auto-mobile-junit-runner SNAPSHOT artifacts.
        // These JARs are committed to the repo so CI can resolve them without mavenLocal.
        maven {
            name = "LocalLibs"
            url = uri("libs/maven")
        }
        mavenLocal()
        google()
        mavenCentral()
    }
}

plugins {
    // Applied here (not via CI init script injection) to avoid classloader conflicts between
    // the Develocity agent and AGP 9.0 when the configuration cache is cold.
    id("com.gradle.develocity") version "4.3.2"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "Android Build"

include(":app")
