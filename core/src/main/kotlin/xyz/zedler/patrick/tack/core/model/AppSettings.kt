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

data class AppSettings(
  // Appearance
  val language: String? = null, // null means "system"
  val useDynamicColors: Boolean = true,
  val themeHue: Float = 200f,
  val theme: AppTheme = AppTheme.SYSTEM,
  val contrast: AppContrast = AppContrast.STANDARD,
  // Behavior
  val haptic: Boolean = true,
  val vibrationIntensity: String = "auto",
  val reduceAnim: Boolean = false,
  // Instrument
  val sound: String = "sine",
  val gain: Int = 0,
  val latency: Long = 0L,
  val beatMode: String = "all",
  val flashlight: String = "off",
  val flashScreen: String = "off",
  val keepAwake: String = "while_playing",
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