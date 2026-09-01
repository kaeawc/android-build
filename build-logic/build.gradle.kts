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
plugins { `kotlin-dsl` }

// Precompiled convention plugins need the Kotlin Gradle plugin and AGP on the
// classpath so they can reference KotlinCompile and the Android extensions. The
// Kotlin DSL compiles on the build's JDK; pin its toolchain to the project's Java
// target for consistency with the modules these plugins configure.
kotlin { jvmToolchain(libs.versions.build.java.target.get().toInt()) }

dependencies {
    implementation(libs.kgp)
    implementation(libs.agp)
    // Lets androidbuild.android-compose apply the Compose compiler plugin by id.
    implementation(libs.compose.compiler.plugin)
    // Artifact Swap's publish plugin, applied per-module by androidbuild.publish. Its
    // settings plugin can auto-apply this, but that needs the class on the settings
    // classpath; applying it manually via a convention plugin is the supported
    // alternative and keeps it on the module classpath where it belongs.
    implementation(libs.artifactswap.publish.plugin)
    // gradle-plugin provides xyz.block.gradle.isArtifactSwapActive and the BOM build
    // service, both used by SwappableProjectDependencies; it also satisfies the publish
    // plugin's compileOnly references at runtime.
    implementation(libs.artifactswap.gradle.plugin)
    // gradle-utils provides the artifactSwapCoordinates path->artifactId helper, imported
    // by SwappableProjectDependencies so consumer coordinates stay in lockstep with what
    // upstream's publish plugin and BOM use.
    implementation(libs.artifactswap.gradle.utils)
}
