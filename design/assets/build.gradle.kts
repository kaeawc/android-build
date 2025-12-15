plugins {
  alias(libs.plugins.android.library)
//  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "dev.jasonpearson.design.assets"
  compileSdk = libs.versions.build.android.compileSdk.get().toInt()
  buildToolsVersion = libs.versions.build.android.buildTools.get()

  defaultConfig {
    minSdk = libs.versions.build.android.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.build.java.target.get())
  }
}

//version = "0.0.1-SNAPSHOT"
//group = "dev.jasonpearson.playground"
//
//mavenPublishing {
//  coordinates(group.toString(), "design-assets", version.toString())
//}
