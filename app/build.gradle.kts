import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.jasonpearson.android"
    compileSdk = libs.versions.build.android.compileSdkVersion.get().toInt()
    buildToolsVersion = libs.versions.build.android.buildToolsVersion.get()

    defaultConfig {
        applicationId = "dev.jasonpearson.android"
        minSdk = libs.versions.build.android.minSdkVersion.get().toInt()
        targetSdk = libs.versions.build.android.targetSdkVersion.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        sourceCompatibility = JavaVersion.toVersion(libs.versions.build.java.targetVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.build.java.targetVersion.get())
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.build.compose.kotlinCompilerVersion.get()
        useLiveLiterals = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain {
        vendor.set(JvmVendorSpec.AZUL)
        languageVersion.set(JavaLanguageVersion.of(libs.versions.build.java.targetVersion.get().toInt()))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        languageVersion = libs.versions.build.kotlin.languageVersion.get()
        jvmTarget = libs.versions.build.java.targetVersion.get()
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.time.ExperimentalTime,kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-Xcontext-receivers"
        )
    }
}

dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)

    testImplementation(libs.bundles.unit.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.ui.espresso.test)

    debugImplementation(libs.bundles.compose.ui.debug)

    // Needed for reading Java 19+ class files due to JVM target higher than 11
    implementation(platform(libs.asm.bom))
    coreLibraryDesugaring(libs.desugar)
}
