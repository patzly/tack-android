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

data class MetronomeState(
  val isPlaying: Boolean = false,
  val isCountingIn: Boolean = false,
  val tempo: Int = 120,
  val timerProgress: Float = 0f,
  val timerBarIndex: Int = 0,
  val timerBeatIndex: Int = 0,
  val timerSubIndex: Int = 0,
  val elapsedTime: Long = 0,
  val currentSongId: String? = null,
  val currentPartIndex: Int = 0,
  val isMuted: Boolean = false,
  val isHapticPossible: Boolean = true
)
