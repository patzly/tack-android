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

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "xyz.zedler.patrick.tack"
  compileSdk = 37

  defaultConfig {
    applicationId = "xyz.zedler.patrick.tack"
    minSdk = 23
    targetSdk = 37
    versionCode = 420 // last number is 0 for app release
    versionName = "6.3.2"

    vectorDrawables.generatedDensities?.clear()
  }

  androidResources {
    localeFilters += listOf(
      "en", "cs", "de", "es", "es-rCL", "fr", "in", "ja", "ko", "nl",
      "pt-rBR", "ru", "tr", "zh-rCN", "zh-rHK", "zh-rTW"
    )
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      storeFile = if (keystorePath != null) file(keystorePath) else null
      storePassword = System.getenv("KEYSTORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS")
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      val releaseSigningConfig = signingConfigs.getByName("release")
      if (releaseSigningConfig.storeFile?.exists() == true) {
        signingConfig = releaseSigningConfig
      } else {
        println("Keystore not found, building unsigned release.")
      }
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      isDebuggable = false
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  lint {
    abortOnError = false
    disable += "MissingTranslation"
  }

  bundle {
    storeArchive {
      enable = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_21
  }

  dependenciesInfo {
    includeInApk = false
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(project(":core"))
  implementation(platform(libs.compose.bom))
  implementation(libs.core)
  implementation(libs.appcompat)
  implementation(libs.ui)
  implementation(libs.compose.material)
  implementation(libs.ui.tooling.preview)
  implementation(libs.compose.activity)
  implementation(libs.materialKolor)

  // Testing
  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.kotlinx.coroutines.test)
}
