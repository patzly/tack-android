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

package xyz.zedler.patrick.tack.presentation.state

import xyz.zedler.patrick.tack.Constants.Def
import xyz.zedler.patrick.tack.presentation.navigation.Screen

data class MainState(
  val currentRoute: String = Screen.Main.route,
  val isPlaying: Boolean = false,
  val tempo: Int = Def.TEMPO,
  val tempoChangedByPicker: Boolean = false,
  val animateTempoChange: Boolean = true,
  val beats: List<String> = Def.BEATS.split(","),
  val beatTriggers: List<Boolean> = List((Def.BEATS.split(",")).size) { false },
  val subdivisions: List<String> = Def.SUBDIVISIONS.split(","),
  val subdivisionTriggers: List<Boolean> =
    List((Def.SUBDIVISIONS.split(",")).size) { false },
  val bookmarks: List<Bookmark> = listOf(),
  val beatModeVibrate: Boolean = Def.BEAT_MODE_VIBRATE,
  val alwaysVibrate: Boolean = Def.ALWAYS_VIBRATE,
  val strongVibration: Boolean = Def.STRONG_VIBRATION,
  val gain: Int = Def.GAIN,
  val sound: String = Def.SOUND,
  val ignoreFocus: Boolean = Def.IGNORE_FOCUS,
  val latency: Long = Def.LATENCY,
  val keepAwake: Boolean = Def.KEEP_AWAKE,
  val reduceAnim: Boolean = Def.REDUCE_ANIM,
  val flashScreen: Boolean = Def.FLASH_SCREEN,
  val flash: Boolean = false,
  val flashStrong: Boolean = false,
  val showPermissionDialog: Boolean = false,
  val startedWithGain: Boolean = false
)