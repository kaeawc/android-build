plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    //  alias(libs.plugins.mavenPublish)
}

android {
    namespace = "dev.jasonpearson.experimentation"
    compileSdk = libs.versions.build.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.build.android.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.build.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
    }
    //  kotlinOptions {
    //    jvmTarget = libs.versions.build.java.target.get()
    //    languageVersion = libs.versions.build.kotlin.language.get()
    //  }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Storage module
    implementation(projects.storage)

    // Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime)

    // Lifecycle compose integration
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}

// version = "0.0.1-SNAPSHOT"
// group = "dev.jasonpearson.playground"
//
// mavenPublishing {
//  coordinates(group.toString(), "experimentation", version.toString())
// }
