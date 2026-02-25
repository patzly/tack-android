/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

configure<ApplicationExtension> {
  namespace = "xyz.zedler.patrick.tack"
  compileSdk = 36

  defaultConfig {
    applicationId = "xyz.zedler.patrick.tack"
    minSdk = 26
    targetSdk = 36
    versionCode = 311 // last number is 1 for wear release
    versionName = "6.2.0"

    externalNativeBuild {
      cmake {
        arguments("-DANDROID_STL=c++_shared")
      }
    }
  }

  androidResources {
    localeFilters += listOf(
      "en", "cs", "de",  "es", "es-rCL", "fr", "in", "ja", "ko", "nl", "pt-rBR", "ru", "tr",
      "zh-rCN","zh-rHK", "zh-rTW"
    )
  }

  signingConfigs {
    create("release") {
      storeFile = rootProject.extra["keystoreFile"] as File
      storePassword = rootProject.extra["keystorePassword"] as String
      keyAlias = rootProject.extra["keyAlias"] as String
      keyPassword = rootProject.extra["keyPassword"] as String
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_21
  }
  lint {
    abortOnError = false
    disable += listOf("MissingTranslation")
  }
  buildFeatures {
    compose = true
    prefab = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    jniLibs.useLegacyPackaging = false
  }
  dependenciesInfo {
    includeInApk = false
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
    }
  }
  ndkVersion = "29.0.14206865"
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.preference)
  implementation(libs.core.ktx)
  implementation(libs.oboe)
  implementation(libs.lifecycle.service)
  implementation(libs.splashscreen)
  implementation(libs.ui)
  implementation(libs.ui.tooling.preview)
  implementation(libs.wear.tooling)
  implementation(libs.wear.ongoing)
  implementation(libs.wear.input)
  implementation(libs.compose.material)
  implementation(libs.compose.material.wear)
  implementation(libs.compose.foundation)
  implementation(libs.compose.runtime)
  implementation(libs.compose.animation.graphics)
  implementation(libs.compose.navigation)
  implementation(libs.compose.constraintlayout)
  implementation(libs.compose.activity)
  debugImplementation(libs.ui.tooling.preview)
  debugImplementation(libs.ui.test.manifest)
  debugImplementation(libs.ui.tooling)
}