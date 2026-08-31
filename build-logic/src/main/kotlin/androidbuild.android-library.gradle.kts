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
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

// Shared configuration for every `com.android.library` module: SDK levels and Java
// source/target sourced from the single version catalog, plus the shared Kotlin
// compiler config via androidbuild.kotlin-common. Each module still declares its
// own `android { namespace = ... }` since namespaces must be unique.
plugins {
    id("com.android.library")
    id("androidbuild.kotlin-common")
    id("androidbuild.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun version(name: String) = libs.findVersion(name).get().requiredVersion

val javaTarget = JavaVersion.toVersion(version("build-java-target"))

extensions.configure<LibraryExtension> {
    compileSdk = version("build-android-compileSdk").toInt()
    buildToolsVersion = version("build-android-buildTools")

    defaultConfig { minSdk = version("build-android-minSdk").toInt() }

    compileOptions {
        sourceCompatibility = javaTarget
        targetCompatibility = javaTarget
    }
}
