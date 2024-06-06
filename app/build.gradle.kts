import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.graphAssertion)
    alias(libs.plugins.publish)
    alias(libs.plugins.sortDependencies)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

moduleGraphAssert {
    maxHeight = 8
    allowed = arrayOf(
        ":app -> :.*",
        ":feature:.* -> :subsystem:.*",
        ":feature:.* -> :foundation:.*",
        ":subsystem:.* -> :foundation:.*",
        ":subsystem:.* -> :core:.*",
        ":foundation:.* -> :core:.*",
        ":core:.* -> :core:.*"
    )
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
    // buildToolsVersion = libs.versions.build.android.buildTools.get()

    defaultConfig {
        applicationId = "dev.jasonpearson.android"
        minSdk = libs.versions.build.android.minSdk.get().toInt()
        targetSdk = libs.versions.build.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
         * Normally we would only want to target Java 8 or 11, but since minSdk for this project
         * is as high as I want it to be I can target more recent JDK versions and use
         * desugaring + ASM to ensure older Android devices can run this just fine.
         */
        sourceCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        useLiveLiterals = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.build.kotlin.language.get().replace(".", "_")}"))
        jvmTarget.set(JvmTarget.valueOf("JVM_${libs.versions.build.java.target.get()}"))
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.time.ExperimentalTime,kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlin.ExperimentalUnsignedTypes",
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-Xcontext-receivers",
            )
        )
    }
}

val ktfmtVersion = libs.versions.build.gradle.ktfmt.get()
val externalFiles = listOf("MemoizedSequence").map { "src/**/$it.kt" }

configure<SpotlessExtension> {
    format("misc") {
        target("*.md", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target("src/**/*.kt")
        targetExclude(externalFiles)
        ktfmt(ktfmtVersion).dropboxStyle()
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(rootProject.file("spotless/spotless.kt"))
        targetExclude("**/spotless.kt", "**/Aliases.kt", *externalFiles.toTypedArray())
    }
    format("kotlinExternal", KotlinExtension::class.java) {
        target(externalFiles)
        ktfmt(ktfmtVersion).dropboxStyle()
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("**/spotless.kt", "**/Aliases.kt")
    }
    kotlinGradle {
        target("src/**/*.kts")
        ktfmt(ktfmtVersion).dropboxStyle()
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(
            rootProject.file("spotless/spotless.kt"),
            "(import|plugins|pluginManagement|dependencies)"
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    // Needed for reading Java 19+ class files due to JVM target higher than 11
    implementation(platform(libs.asm.bom))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.bundles.compose.ui)

    debugImplementation(libs.bundles.compose.ui.debug)

    testImplementation(libs.bundles.unit.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.ui.espresso.test)
}
