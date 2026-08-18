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

package xyz.zedler.patrick.tack.core.model

import android.os.Build

data class AppSettings(
  // Appearance
  val language: String? = null, // null means "system"
  val color: AppColor =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AppColor.DYNAMIC else AppColor.STATIC,
  val colorHue: Float = 154f,
  val theme: AppTheme = AppTheme.SYSTEM,
  val contrast: AppContrast = AppContrast.STANDARD,
  // Behavior
  val haptic: Boolean = true,
  val vibrationIntensity: VibrationIntensity = VibrationIntensity.AUTO,
  val reduceAnim: Boolean = false,
  // Instrument
  val sound: Sound = Sound.SINE,
  val gain: Int = 0,
  val latency: Long = 0L,
  val beatMode: BeatMode = BeatMode.ALL,
  val flashlight: FlashStrength = FlashStrength.OFF,
  val flashScreen: FlashStrength = FlashStrength.OFF,
  val keepAwake: KeepAwakeMode = KeepAwakeMode.WHILE_PLAYING,
  val ignoreFocus: Boolean = false,
  // UI
  val showElapsed: Boolean = false,
  val resetTimerOnStop: Boolean = false,
  val permNotification: Boolean = false,
  val activeBeat: Boolean = true,
  val bigTimeText: Boolean = false,
  val bigLogo: Boolean = false,
  // Misc
  val checkUnlockKey: Boolean = true
)

enum class AppColor(val key: String) {
  DYNAMIC("dynamic"),
  STATIC("static");

  companion object {
    fun fromKey(key: String): AppColor = entries.find { it.key == key } ?: DYNAMIC
  }
}

enum class AppTheme(val key: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromKey(key: String): AppTheme = entries.find { it.key == key } ?: SYSTEM
  }
}

enum class AppContrast(val key: String) {
  STANDARD("standard"),
  MEDIUM("medium"),
  HIGH("high");

  companion object {
    fun fromKey(key: String): AppContrast = entries.find { it.key == key } ?: STANDARD
  }
}

enum class VibrationIntensity(val key: String) {
  AUTO("auto"),
  SOFT("soft"),
  STRONG("strong");

  companion object {
    fun fromKey(key: String): VibrationIntensity = entries.find { it.key == key } ?: AUTO
  }
}

enum class Sound(val key: String) {
  SINE("sine"),
  WOOD("wood"),
  MECHANICAL("mechanical"),
  BEATBOXING_1("beatboxing_1"),
  BEATBOXING_2("beatboxing_2"),
  HANDS("hands"),
  FOLDING("folding");

  companion object {
    fun fromKey(key: String): Sound = entries.find { it.key == key } ?: SINE
  }
}

enum class BeatMode(val key: String) {
  ALL("all"),
  SOUND("sound"),
  VIBRATION("vibration");

  companion object {
    fun fromKey(key: String): BeatMode = entries.find { it.key == key } ?: ALL
  }
}

enum class FlashStrength(val key: String) {
  OFF("off"),
  SUBTLE("subtle"),
  STRONG("strong");

  companion object {
    fun fromKey(key: String): FlashStrength = entries.find { it.key == key } ?: OFF
  }
}

enum class KeepAwakeMode(val key: String) {
  ALWAYS("always"),
  WHILE_PLAYING("while_playing"),
  NEVER("never");

  companion object {
    fun fromKey(key: String): KeepAwakeMode = entries.find { it.key == key } ?: WHILE_PLAYING
  }
}
